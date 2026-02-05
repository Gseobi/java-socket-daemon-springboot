package com.github.gseobi.daemon.socket.service;

import com.github.gseobi.daemon.socket.config.DaemonProperties;
import com.github.gseobi.daemon.socket.config.ProviderConfigManager;
import com.github.gseobi.daemon.socket.config.model.ProviderConfig;
import com.github.gseobi.daemon.socket.crypto.DecryptUtil;
import com.github.gseobi.daemon.socket.crypto.EncryptUtil;
import com.github.gseobi.daemon.socket.dto.db.ProcedureRequest;
import com.github.gseobi.daemon.socket.job.JobRepository;
import com.github.gseobi.daemon.socket.persistence.mapper.SocketMapper;
import com.github.gseobi.daemon.socket.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocketService {

//  @Value("${daemon.nas-ip}")
//  private String nasIp;
//  @Value("${daemon.socket.retry-count}")
//  private int retryCount;
//  @Value("${daemon.socket.jitter-ms}")
//  private int jitterMs;
//  @Value("${daemon.socket.connect-timeout-ms}")
//  private int connectTimeoutMs;
//  @Value("${daemon.socket.read-timeout-ms}")
//  private int readTimeoutMs;
//  @Value("${daemon.socket.retry-backoff-ms}")
//  private int retryBackoffMs;
//  private final SocketMapper socketMapper;

    private final JobRepository jobRepository;
    private final DaemonProperties props;

    /**
     * Stored Procedure 호출: 다음 처리할 요청 fetch
     */
    public ProcedureRequest processRequest(String logId) {
//      ProcedureRequest req = new ProcedureRequest();
//      req.setI_NAS_IP(nasIp);
//
//      this.socketMapper.callSocketRequestProc(req);
//      log.info("[{}] CALL request-proc: {}", logId, req);
//
//      return req;

        return jobRepository.fetchNext(logId);
    }

    /**
     * Stored Procedure 호출: 결과 저장
     */
    public void processResponse(String logId, String seq, String retCd, String retMsg) {
//       ProcedureResponse res = new ProcedureResponse();
//       res.setI_NAS_IP(nasIp);
//       res.setI_SEQ(seq);
//       res.setI_RES_CD(retCd);
//       res.setI_RES_MSG(retMsg);
//
//       this.socketMapper.callSocketResponseProc(res);
//       log.info("[{}] CALL response-proc: {}", logId, res);
        jobRepository.saveResult(logId, seq, retCd, retMsg);
    }

    /**
     * Provider Socket Connection (Encrypt -> Socket -> Decrypt)
     */
    public JSONObject providerSocketConnect(String logId, String seq, String providerCode, String reqData) {
        ProviderConfig cfg = ProviderConfigManager.getConfig(providerCode);

        if (cfg == null) {
            log.warn("[{}][{}] Config not found. provider={}", logId, seq, providerCode);
            JSONObject r = new JSONObject();
            r.put("resCd", "8888");
            r.put("resStr", "CONFIG_NOT_FOUND");
            return r;
        }

        // 1) Encrypt
        String encryptedPayload;
        try {
            encryptedPayload = EncryptUtil.encrypt(
                    reqData,
                    cfg.getEncType(),
                    cfg.getEncKey(),
                    cfg.getEncIv(),
                    cfg.getEncOut()
            );
            log.info("[{}][{}] Encrypt success.", logId, seq);
        } catch (Exception e) {
            log.warn("[{}][{}] Encrypt fail.", logId, seq, e);
            JSONObject r = new JSONObject();
            r.put("resCd", "8888");
            r.put("resStr", "ENCRYPT_FAIL");
            return r;
        }

        // 2) Socket I/O
        JSONObject socketRes = socketConnect(
                logId,
                seq,
                cfg.getIp(),
                cfg.getPort(),
                applyProtocolFraming(encryptedPayload, cfg)
        );

        // 3) Decrypt only if success
        String resCd = JsonUtil.getString(socketRes, "resCd", "9999");
        String resStr = JsonUtil.getString(socketRes, "resStr", "COMM_ERROR");

        if (!"0000".equals(resCd)) return socketRes;

        try {
            String decrypted = DecryptUtil.decrypt(resStr, cfg.getEncType(), cfg.getEncKey(), cfg.getEncIv());
            socketRes.put("resStr", decrypted);
            log.info("[{}][{}] Decrypt success.", logId, seq);
        } catch (Exception e) {
            log.warn("[{}][{}] Decrypt fail.", logId, seq, e);
            socketRes.put("resCd", "8888");
            socketRes.put("resStr", "DECRYPT_FAIL");
        }

        return socketRes;
    }

    private JSONObject socketConnect(String logId, String seq, String ip, int port, String payload) {
        JSONObject result = new JSONObject();
        Random random = new Random();

        String resCd = "9999";
        String resStr = "INITIAL_ERROR";

        Socket socket = null;
        try {
            int retry = 0;
            int retryCount = props.getSocket().getRetryCount();
            while (retry < retryCount) {
                socket = new Socket();
                socket.setReuseAddress(true);

                int jitterMs = props.getSocket().getJitterMs();
                if (jitterMs > 0) {
                    Thread.sleep(random.nextInt(jitterMs));
                }
                try {
                    int connectTimeoutMs = props.getSocket().getConnectTimeoutMs();
                    socket.connect(
                            new InetSocketAddress(ip, port),
                            connectTimeoutMs
                    );
                    break;
                } catch (Exception e) {
                    retry++;
                    if (retry >= retryCount) throw e;

                    safeClose(socket);
                    socket = null;

                    int retryBackoffMs = props.getSocket().getReadTimeoutMs();
                    Thread.sleep(retryBackoffMs);
                }
            }

            int readTimeoutMs = props.getSocket().getReadTimeoutMs();
            socket.setSoTimeout(readTimeoutMs);

            try (OutputStream out = socket.getOutputStream();
                 InputStream in = socket.getInputStream()) {

                out.write(payload.getBytes(StandardCharsets.UTF_8));
                out.flush();
                log.info("[{}][{}] Data sent. providerSocket={}:{}", logId, seq, ip, port);

                String response = readResponse(in);
                if (response != null && !response.isBlank()) {
                    resCd = "0000";
                    resStr = response.trim();
                } else {
                    resCd = "9999";
                    resStr = "EMPTY_RESPONSE";
                }
            }

        } catch (SocketTimeoutException te) {
            resCd = "7777";
            resStr = "SOCKET_TIMEOUT";
            log.warn("[{}][{}] Socket timeout.", logId, seq);
        } catch (Exception e) {
            resCd = "9999";
            resStr = "COMM_ERROR";
            log.warn("[{}][{}] Socket error.", logId, seq, e);
        } finally {
            safeClose(socket);
            result.put("resCd", resCd);
            result.put("resStr", resStr);
            log.info("[{}][{}] Socket result: {}", logId, seq, result);
        }

        return result;
    }

    private String readResponse(InputStream in) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int readLen;

        while (true) {
            try {
                readLen = in.read(buffer);

                if (readLen == -1) break;

                baos.write(buffer, 0, readLen);

                Thread.sleep(50);

                if (in.available() == 0) break;
            } catch (SocketTimeoutException te) {
                if (baos.size() > 0) break;
                throw te;
            }
        }

        return baos.toString(StandardCharsets.UTF_8);
    }

    private String applyProtocolFraming(String encrypted, ProviderConfig cfg) {
        // Provider별 framing 필요 시 ProviderConfig에 prefix/suffix 추가 가능
        return encrypted + "\n";
    }

    private void safeClose(Socket socket) {
        if (socket == null) return;
        try {
            if (!socket.isClosed()) socket.close();
        } catch (Exception ignore) {}
    }
}

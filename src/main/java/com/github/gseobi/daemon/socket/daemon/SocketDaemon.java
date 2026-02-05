package com.github.gseobi.daemon.socket.daemon;

import com.github.gseobi.daemon.socket.SocketDaemonApplication;
import com.github.gseobi.daemon.socket.config.ProviderConfigManager;
import com.github.gseobi.daemon.socket.dto.db.ProcedureRequest; // (네가 앞에서 만든 포폴용 DTO 기준)
import com.github.gseobi.daemon.socket.service.SocketService;
import com.github.gseobi.daemon.socket.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SocketDaemon implements Daemon, Runnable {

    @Value("${daemon.poll-interval-ms}")
    private long pollIntervalMs;

    private volatile boolean running = false;

    private Thread mainThread;
    private ConfigurableApplicationContext context;
    private SocketService socketService;
    private Executor socketExecutor;

    @Override
    public void init(DaemonContext daemonContext) {
        log.info("[SocketDaemon] Initializing...");

        SpringApplication app = new SpringApplication(SocketDaemonApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);

        this.context = app.run(daemonContext.getArguments());
        this.socketService = context.getBean(SocketService.class);
        this.socketExecutor = (Executor) context.getBean("socketExecutor");

        // Provider 설정 파일 로딩 (기존 MVNO 설정 로딩의 포폴용 치환)
        ProviderConfigManager.initAllConfigs();

        this.mainThread = new Thread(this, "socket-daemon-main");
        this.running = false;

        log.info("[SocketDaemon] Initialized.");
    }

    @Override
    public void start() {
        log.info("[SocketDaemon] Starting...");
        this.running = true;
        this.mainThread.start();
        log.info("[SocketDaemon] Started.");
    }

    @Override
    public void stop() {
        log.info("[SocketDaemon] Stopping...");
        this.running = false;

        if (mainThread != null) {
            mainThread.interrupt();
            try {
                mainThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[SocketDaemon] Main thread join interrupted.");
            }
        }

        shutdownExecutor();

        if (context != null) {
            context.close();
        }

        log.info("[SocketDaemon] Stopped.");
    }

    @Override
    public void destroy() {
        log.info("[SocketDaemon] Destroying...");
        log.info("[SocketDaemon] Destroyed.");
    }

    @Override
    public void run() {
        log.info("[SocketDaemon] Main loop started.");

        while (running && !Thread.currentThread().isInterrupted()) {

            final String logId = generateLogId();

            try {
                ProcedureRequest job = socketService.processRequest(logId);

                long intervalMs = parseInterval(job != null ? job.getO_TIMER() : null, pollIntervalMs);

                if (job == null || !"0000".equals(job.getO_RET_CD())) {
                    sleepSafely(intervalMs);
                    continue;
                }

                final String seq = job.getO_SEQ();
                final String providerCode = job.getO_PROVIDER_CODE();
                final String requestData = job.getO_REQ_DATA();

                socketExecutor.execute(() -> handleSocketTask(logId, seq, providerCode, requestData));

                sleepSafely(intervalMs);

            } catch (Exception e) {
                log.error("[SocketDaemon] Main loop exception. logId={}", logId, e);
                sleepSafely(1000L);
            }
        }

        log.info("[SocketDaemon] Main loop finished.");
    }

    private void handleSocketTask(String logId, String seq, String providerCode, String requestData) {
        try {
            JSONObject resJson = socketService.providerSocketConnect(logId, seq, providerCode, requestData);

            if (resJson != null) {
                String resCd = JsonUtil.getString(resJson, "resCd", "9999");
                String resMsg = JsonUtil.getString(resJson, "resStr", "");
                socketService.processResponse(logId, seq, resCd, resMsg);
            } else {
                socketService.processResponse(logId, seq, "9999", "EMPTY_RESPONSE");
            }

        } catch (Exception e) {
            log.error("[SocketDaemon] Task exception. logId={}, seq={}, provider={}", logId, seq, providerCode, e);
            try {
                socketService.processResponse(logId, seq, "9999", "EXCEPTION");
            } catch (Exception ignore) {
                log.warn("[SocketDaemon] Failed to persist error response. logId={}, seq={}", logId, seq);
            }
        }
    }

    private void shutdownExecutor() {
        if (socketExecutor instanceof ThreadPoolExecutor threadPoolExecutor) {
            log.info("[SocketDaemon] Shutting down executor. active={}", threadPoolExecutor.getActiveCount());
            threadPoolExecutor.shutdown();
            try {
                if (!threadPoolExecutor.awaitTermination(20, TimeUnit.SECONDS)) {
                    log.warn("[SocketDaemon] Executor did not terminate. Forcing shutdown.");
                    threadPoolExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                threadPoolExecutor.shutdownNow();
            }
            log.info("[SocketDaemon] Executor shutdown complete.");
        }
    }

    private String generateLogId() {
        int logIdInt = Math.abs(UUID.randomUUID().hashCode() % 100000000);
        return String.format("%08d", logIdInt);
    }

    private long parseInterval(String timerValue, long defaultMs) {
        if (timerValue == null || timerValue.isBlank()) return defaultMs;
        try {
            return Long.parseLong(timerValue.trim());
        } catch (Exception e) {
            return defaultMs;
        }
    }

    private void sleepSafely(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

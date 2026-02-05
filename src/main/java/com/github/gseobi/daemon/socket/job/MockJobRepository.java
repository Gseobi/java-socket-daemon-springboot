package com.github.gseobi.daemon.socket.job;

import com.github.gseobi.daemon.socket.config.DaemonProperties;
import com.github.gseobi.daemon.socket.dto.db.ProcedureRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class MockJobRepository implements JobRepository {

    private final DaemonProperties props;
    private final AtomicLong seqGen = new AtomicLong(1);

    @Override
    public ProcedureRequest fetchNext(String logId) {
        ProcedureRequest req = new ProcedureRequest();
        req.setI_NAS_IP(props.getNasIp());

        req.setO_RET_CD("0000");
        req.setO_TIMER(String.valueOf(props.getPollIntervalMs()));
        req.setO_SEQ(String.valueOf(seqGen.getAndIncrement()));

        req.setO_PROVIDER_CODE("PROVIDER_A");

        req.setO_REQ_DATA("{\"hello\":\"world\",\"timestamp\":\"" + System.currentTimeMillis() + "\"}");

        log.info("[{}] MockJobRepository issued job seq={}", logId, req.getO_SEQ());
        return req;
    }

    @Override
    public void saveResult(String logId, String seq, String resCd, String resMsg) {
        log.info("[{}] Mock saveResult seq={}, resCd={}, resMsg={}", logId, seq, resCd, resMsg);
    }
}

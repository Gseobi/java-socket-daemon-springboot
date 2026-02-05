package com.github.gseobi.daemon.socket.job;

import com.github.gseobi.daemon.socket.config.DaemonProperties;
import com.github.gseobi.daemon.socket.dto.db.ProcedureRequest;
import com.github.gseobi.daemon.socket.dto.db.ProcedureResponse;
import com.github.gseobi.daemon.socket.persistence.mapper.SocketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!local")
@RequiredArgsConstructor
public class DbJobRepository implements JobRepository {

    private final SocketMapper socketMapper;
    private final DaemonProperties props;

    @Override
    public ProcedureRequest fetchNext(String logId) {
        ProcedureRequest req = new ProcedureRequest();
        req.setI_NAS_IP(props.getNasIp());
        socketMapper.callSocketRequestProc(req);
        return req;
    }

    @Override
    public void saveResult(String logId, String seq, String resCd, String resMsg) {
        ProcedureResponse res = new ProcedureResponse();
        res.setI_NAS_IP(props.getNasIp());
        res.setI_SEQ(seq);
        res.setI_RES_CD(resCd);
        res.setI_RES_MSG(resMsg);
        socketMapper.callSocketResponseProc(res);
    }
}

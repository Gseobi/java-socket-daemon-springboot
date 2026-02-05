package com.github.gseobi.daemon.socket.job;

import com.github.gseobi.daemon.socket.dto.db.ProcedureRequest;

public interface JobRepository {
    ProcedureRequest fetchNext(String logId);
    void saveResult(String logId, String seq, String resCd, String resMsg);
}

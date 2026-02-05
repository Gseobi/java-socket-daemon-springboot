package com.github.gseobi.daemon.socket.persistence.mapper;

import com.github.gseobi.daemon.socket.dto.db.ProcedureRequest;
import com.github.gseobi.daemon.socket.dto.db.ProcedureResponse;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SocketMapper {
    void callSocketRequestProc(ProcedureRequest req);
    void callSocketResponseProc(ProcedureResponse res);
}

package com.github.gseobi.daemon.socket.dto.db;

import lombok.Data;

/**
 * - I_: Input parameter
 * - O_: Output parameter
 */
@Data
public class ProcedureResponse {

    private String I_NAS_IP;
    private String I_SEQ;
    private String I_RES_CD;
    private String I_RES_MSG;

    private String O_RET_CD;
    private String O_RET_MSG;
}

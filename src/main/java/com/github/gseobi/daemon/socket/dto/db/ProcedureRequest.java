package com.github.gseobi.daemon.socket.dto.db;

import lombok.Data;

/**
 * - I_: Input parameter
 * - O_: Output parameter
 */
@Data
public class ProcedureRequest {

    private String I_NAS_IP;

    private String O_RET_CD;
    private String O_TIMER;
    private String O_SEQ;
    private String O_PROVIDER_CODE;
    private String O_REQ_DATA;
}

package com.github.gseobi.daemon.socket.config.model;

import com.github.gseobi.daemon.socket.crypto.constant.EncryptionType;
import com.github.gseobi.daemon.socket.crypto.constant.OutputFormat;
import lombok.Data;

@Data
public class ProviderConfig {
    private String ip;
    private int port;

    private EncryptionType encType;
    private String encKey;
    private String encIv;
    private OutputFormat encOut;

    private boolean encryptOutbound;
}

package com.github.gseobi.daemon.socket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "daemon")
public class DaemonProperties {

    private String nasIp = "127.0.0.1";
    private long pollIntervalMs = 2000;

    private Socket socket = new Socket();
    private Conf conf = new Conf();

    @Data
    public static class Socket {
        private int connectTimeoutMs = 3000;
        private int readTimeoutMs = 30000;
        private int retryCount = 3;
        private int jitterMs = 200;
        private int retryBackoffMs = 500;
    }

    @Data
    public static class Conf {
        private String dir = "config/provider";
    }
}

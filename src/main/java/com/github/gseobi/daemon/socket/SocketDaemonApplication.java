package com.github.gseobi.daemon.socket;

import com.github.gseobi.daemon.socket.config.DaemonProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(DaemonProperties.class)
@SpringBootApplication
public class SocketDaemonApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SocketDaemonApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}

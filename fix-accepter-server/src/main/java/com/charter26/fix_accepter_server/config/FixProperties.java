package com.charter26.fix_accepter_server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fix")
public class FixProperties {

    private String configPath;
    private String host;
    private int port;

    public String getConfigPath() {
        return configPath;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }
}
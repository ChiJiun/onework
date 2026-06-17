package com.example.meetingroom.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin-login")
public class AdminLoginProperties {

    private List<String> allowedIps = List.of("127.0.0.1", "::1", "0:0:0:0:0:0:0:1");

    public List<String> getAllowedIps() {
        return allowedIps;
    }

    public void setAllowedIps(List<String> allowedIps) {
        this.allowedIps = allowedIps;
    }
}

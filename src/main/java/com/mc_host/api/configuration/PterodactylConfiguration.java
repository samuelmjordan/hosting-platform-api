package com.mc_host.api.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "pterodactyl")
public class PterodactylConfiguration {
    private String apiBase;
    private String apiToken;
    private String clientApiToken;
    private String adminUser;
    private String adminPassword;
    private String passwordKey;
}

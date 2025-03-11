package com.mc_host.api.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "pterodactyl")
public class PterodactylConfiguration {
    private String apiBase;
    private String apiToken;
    private String clientApiToken;
}

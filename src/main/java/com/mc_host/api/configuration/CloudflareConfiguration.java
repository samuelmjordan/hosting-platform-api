package com.mc_host.api.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "cloudflare")
public class CloudflareConfiguration {
    private String apiBase;
    private String apiToken;
    private String originCert;
    private String privateKey;
}

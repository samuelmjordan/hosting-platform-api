package com.mc_host.api.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "ssh")
public class SshConfiguration {
    private String privateKey;
    private String publicKey;
}

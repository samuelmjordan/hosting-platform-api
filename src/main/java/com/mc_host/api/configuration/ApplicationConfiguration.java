package com.mc_host.api.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "application")
public class ApplicationConfiguration {
    private String scheme;
    private String cloudDomain;
    private String nodePublicSuffix;
    private String nodePrivateSuffix;
}

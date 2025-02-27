package com.mc_host.api.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "application")
public class ApplicationConfiguration {
    private String scheme;
    private String domain;
}

package com.mc_host.api.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.clerk.backend_api.Clerk;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "clerk")
public class ClerkConfiguration {
    private String key;

    @Bean
    public Clerk getClient() {
        return Clerk.builder()
            .bearerAuth(this.getKey())
            .build();
    }
}

package com.mc_host.api.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.clerk.backend_api.Clerk;

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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
    
}

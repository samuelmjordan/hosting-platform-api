package com.mc_host.api.configuration;

import com.clerk.backend_api.Clerk;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "clerk")
public class ClerkConfiguration {
    private String key;
    private String signingKey;
    private List<String> validEvents;
    private String jwtUrl;

    @Bean
    public Clerk getClient() {
        return Clerk.builder()
            .bearerAuth(this.getKey())
            .build();
    }

    public Boolean isValidEventType(String type) {
        return validEvents.contains(type);
    }
}

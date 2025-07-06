package com.mc_host.api.configuration.security;

import com.mc_host.api.auth.UserArgumentResolver;
import com.mc_host.api.auth.ValidatedPaymentMethodResolver;
import com.mc_host.api.auth.ValidatedSubscriptionResolver;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Setter
@Configuration
@ConfigurationProperties(prefix = "cors")
public class CorsConfiguration implements WebMvcConfigurer {

    List<String> allowedOrigins;

    @Autowired private UserArgumentResolver userArgumentResolver;
    @Autowired private ValidatedSubscriptionResolver validatedSubscriptionResolver;
    @Autowired private ValidatedPaymentMethodResolver validatedPaymentMethodResolver;

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(allowedOrigins.toArray(new String[0]))
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
            .allowedHeaders("*");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(userArgumentResolver);
        resolvers.add(validatedSubscriptionResolver);
        resolvers.add(validatedPaymentMethodResolver);
    }
}
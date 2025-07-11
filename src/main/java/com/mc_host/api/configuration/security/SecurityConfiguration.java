package com.mc_host.api.configuration.security;

import com.mc_host.api.configuration.ClerkConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfiguration {

	private final ClerkConfiguration clerkConfiguration;

	@Bean
	public SecurityFilterChain filterChain(
		HttpSecurity http
	) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(authz -> authz
				.requestMatchers("/api/**").authenticated()
				.anyRequest().permitAll()
			)
			.oauth2ResourceServer(oauth2 ->
				oauth2.jwt(jwt ->
					jwt.decoder(jwtDecoder())
				)
			);
		return http.build();
	}

	@Bean
	public JwtDecoder jwtDecoder() {
		return NimbusJwtDecoder
			.withJwkSetUri(clerkConfiguration.getJwtUrl())
			.build();
	}
}

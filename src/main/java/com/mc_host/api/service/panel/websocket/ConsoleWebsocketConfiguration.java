package com.mc_host.api.service.panel.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class ConsoleWebsocketConfiguration implements WebSocketConfigurer {

    private final PterodactylProxyHandler pterodactylProxyHandler;

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(pterodactylProxyHandler, "/ws/user/subscription/{subscriptionId}")
            .setAllowedOrigins("*");
    }
}

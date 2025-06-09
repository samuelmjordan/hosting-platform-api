package com.mc_host.api.service.panel.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class ConsoleWebsocketConfiguration implements WebSocketConfigurer {

    private final PterodactylProxyHandler pterodactylProxyHandler;

    public ConsoleWebsocketConfiguration(
        PterodactylProxyHandler pterodactylProxyHandler
    ) {
        this.pterodactylProxyHandler = pterodactylProxyHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(pterodactylProxyHandler, "/ws/user/{userId}/subscription/{subscriptionId}")
            .setAllowedOrigins("*");
    }
}

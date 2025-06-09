package com.mc_host.api.service.panel.websocket;

import java.net.URI;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.mc_host.api.configuration.PterodactylConfiguration;
import com.mc_host.api.controller.panel.ConsoleResource;
import com.mc_host.api.model.resource.pterodactyl.panel.WebsocketCredentials;

@Component
public class PterodactylProxyHandler extends TextWebSocketHandler {
    private static final String PTERODACTYL_SESSION = "PTERODACTYL_SESSION";
    
    private final ConsoleResource consoleResource;
    private final PterodactylConfiguration pterodactylConfiguration;

    public PterodactylProxyHandler(
        ConsoleResource consoleResource,
        PterodactylConfiguration pterodactylConfiguration
    ) {
        this.consoleResource = consoleResource;
        this.pterodactylConfiguration = pterodactylConfiguration;
    }
    
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        String subscriptionId = extractSubscriptionId(session);
        String userId = extractUserId(session);
        
        WebsocketCredentials creds = consoleResource.getWebsocketCredentials(userId, subscriptionId).getBody();
        
        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.set("Origin", pterodactylConfiguration.getApiBase());

        session.getAttributes().put("token", creds.token());
        
        WebSocketSession pteroSession = client.doHandshake(
            new ConsoleWebSocketHandler(session), 
            headers, 
            URI.create(creds.socket())
        ).get();

        session.getAttributes().put(PTERODACTYL_SESSION, pteroSession);
    }
    
    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        WebSocketSession pteroSession = (WebSocketSession) session.getAttributes().get(PTERODACTYL_SESSION);
        if (pteroSession != null && pteroSession.isOpen()) {
            pteroSession.sendMessage(message);
        }
    }
    
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        WebSocketSession pteroSession = (WebSocketSession) session.getAttributes().get(PTERODACTYL_SESSION);
        if (pteroSession != null && pteroSession.isOpen()) {
            pteroSession.close();
        }
    }
    
    private String extractSubscriptionId(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        if (parts.length >= 6) {
            return parts[5];
        }
        throw new IllegalArgumentException("invalid websocket path: " + path);
    }

    private String extractUserId(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        if (parts.length >= 4) {
            return parts[3];
        }
        throw new IllegalArgumentException("invalid websocket path: " + path);
    }
}
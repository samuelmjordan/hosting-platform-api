package com.mc_host.api.service.panel.websocket;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(PterodactylProxyHandler.class);
    private static final String PTERODACTYL_SESSION = "PTERODACTYL_SESSION";
    private static final String USER_ID = "USER_ID";
    private static final String SUBSCRIPTION_ID = "SUBSCRIPTION_ID";
    
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
        
        log.info("establishing pterodactyl connection for user {} subscription {}", userId, subscriptionId);
        
        // store ids for later use
        session.getAttributes().put(USER_ID, userId);
        session.getAttributes().put(SUBSCRIPTION_ID, subscriptionId);
        
        try {
            WebsocketCredentials creds = consoleResource.getWebsocketCredentials(userId, subscriptionId).getBody();
            
            if (creds == null) {
                log.error("received null credentials for user {} subscription {}", userId, subscriptionId);
                session.close(CloseStatus.SERVER_ERROR);
                return;
            }
            
            StandardWebSocketClient client = new StandardWebSocketClient();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.set("Origin", pterodactylConfiguration.getApiBase());

            session.getAttributes().put("token", creds.token());
            
            WebSocketSession pteroSession = client.doHandshake(
                new ConsoleWebSocketHandler(session, consoleResource, userId, subscriptionId), 
                headers, 
                URI.create(creds.socket())
            ).get();

            session.getAttributes().put(PTERODACTYL_SESSION, pteroSession);
            log.info("successfully connected to pterodactyl for user {} subscription {}", userId, subscriptionId);
            
        } catch (Exception e) {
            log.error("failed to connect to pterodactyl for user {} subscription {}", userId, subscriptionId, e);
            session.close(CloseStatus.SERVER_ERROR);
        }
    }
    
    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        WebSocketSession pteroSession = (WebSocketSession) session.getAttributes().get(PTERODACTYL_SESSION);
        if (pteroSession != null && pteroSession.isOpen()) {
            pteroSession.sendMessage(message);
        } else {
            log.warn("attempted to send message but pterodactyl session is closed");
        }
    }
    
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        String userId = (String) session.getAttributes().get(USER_ID);
        String subscriptionId = (String) session.getAttributes().get(SUBSCRIPTION_ID);
        
        log.info("browser connection closed for user {} subscription {}, status: {}", 
                userId, subscriptionId, status);
        
        WebSocketSession pteroSession = (WebSocketSession) session.getAttributes().get(PTERODACTYL_SESSION);
        if (pteroSession != null && pteroSession.isOpen()) {
            pteroSession.close(CloseStatus.NORMAL);
            log.info("closed pterodactyl connection for user {} subscription {}", userId, subscriptionId);
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
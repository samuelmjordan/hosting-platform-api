package com.mc_host.api.service.panel.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.controller.panel.ConsoleResource;
import com.mc_host.api.model.resource.pterodactyl.panel.WebsocketCredentials;
import com.mc_host.api.model.resource.pterodactyl.panel.WebsocketEvent;

import java.util.Map;

public class ConsoleWebSocketHandler extends TextWebSocketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleWebSocketHandler.class);
    private static final String TOKEN_EXPIRING = "token expiring";
    private static final String TOKEN_EXPIRED = "token expired";
    
    private final WebSocketSession browserSession;
    private final ConsoleResource consoleResource;
    private final String userId;
    private final String subscriptionId;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public ConsoleWebSocketHandler(
        WebSocketSession browserSession, 
        ConsoleResource consoleResource,
        String userId,
        String subscriptionId
    ) {
        this.browserSession = browserSession;
        this.consoleResource = consoleResource;
        this.userId = userId;
        this.subscriptionId = subscriptionId;
    }
    
    @Override
    protected void handleTextMessage(@NonNull WebSocketSession pteroSession, @NonNull TextMessage message) throws Exception {
        try {
            String event = objectMapper.readValue(message.getPayload(), WebsocketEvent.class).event();
            
            if (TOKEN_EXPIRING.equals(event)) {
                LOGGER.info("pterodactyl token expiring, refreshing...");
                refreshPterodactylToken(pteroSession);
                return;
            } else if (TOKEN_EXPIRED.equals(event)) {
                LOGGER.warn("pterodactyl token expired, attempting reconnection...");
                forwardToBrowser(message);
                return;
            }
        } catch (Exception e) {
            // not json or doesn't have event field, treat as normal message
        }
        
        forwardToBrowser(message);
    }
    
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession pteroSession) throws Exception {
        String token = (String) browserSession.getAttributes().get("token");
        
        if (token != null) {
            sendAuthMessage(pteroSession, token);
        } else {
            LOGGER.error("no token found in browser session attributes");
        }
    }
    
    private void refreshPterodactylToken(WebSocketSession pteroSession) {
        try {
            WebsocketCredentials newCreds = consoleResource.getWebsocketCredentials(userId, subscriptionId).getBody();
            
            if (newCreds != null) {
                sendAuthMessage(pteroSession, newCreds.token());
                browserSession.getAttributes().put("token", newCreds.token());
                LOGGER.info("successfully refreshed pterodactyl token");
            } else {
                LOGGER.error("received null credentials when refreshing token");
            }
            
        } catch (Exception e) {
            LOGGER.error("failed to refresh pterodactyl token", e);
        }
    }
    
    private void sendAuthMessage(WebSocketSession pteroSession, String token) throws Exception {
        Map<String, Object> authMessage = Map.of(
            "event", "auth",
            "args", new String[]{token}
        );
        
        String authJson = objectMapper.writeValueAsString(authMessage);
        pteroSession.sendMessage(new TextMessage(authJson));
    }
    
    private void forwardToBrowser(TextMessage message) {
        try {
            if (browserSession.isOpen()) {
                browserSession.sendMessage(message);
            }
        } catch (Exception e) {
            LOGGER.error("failed to forward message to browser", e);
        }
    }
}
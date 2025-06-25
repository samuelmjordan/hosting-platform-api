package com.mc_host.api.service.panel.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.controller.api.subscriptions.panel.ConsoleResource;
import com.mc_host.api.model.resource.pterodactyl.panel.WebsocketCredentials;
import com.mc_host.api.model.resource.pterodactyl.panel.WebsocketEvent;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConsoleWebSocketHandler extends TextWebSocketHandler {
    private static final Logger LOGGER = Logger.getLogger(ConsoleWebSocketHandler.class.getName());
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
            WebsocketEvent websocketEvent = objectMapper.readValue(message.getPayload(), WebsocketEvent.class);
            String event = websocketEvent.event();

            if (TOKEN_EXPIRING.equals(event)) {
                refreshPterodactylToken(pteroSession);
                return;
            } else if (TOKEN_EXPIRED.equals(event)) {
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
            LOGGER.log(Level.SEVERE, "no token found in browser session attributes");
        }
    }
    
    private void refreshPterodactylToken(WebSocketSession pteroSession) {
        try {
            WebsocketCredentials newCreds = consoleResource.getWebsocketCredentials(userId, subscriptionId).getBody();
            
            if (newCreds != null) {
                sendAuthMessage(pteroSession, newCreds.token());
                browserSession.getAttributes().put("token", newCreds.token());
            } else {
                LOGGER.log(Level.SEVERE, "received null credentials when refreshing token");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "failed to refresh pterodactyl token", e);
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
            LOGGER.log(Level.SEVERE, "failed to forward message to browser", e);
        }
    }
}
package com.mc_host.api.service.panel.websocket;

import java.net.URI;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.mc_host.api.controller.panel.ConsoleResource;
import com.mc_host.api.model.resource.pterodactyl.panel.WebsocketCredentials;

@Component
public class PterodactylProxyHandler extends TextWebSocketHandler {
    
    private final ConsoleResource consoleResource;

    public PterodactylProxyHandler(ConsoleResource consoleResource) {
        this.consoleResource = consoleResource;
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String subscriptionId = extractSubscriptionId(session);
        String userId = extractUserId(session);
        
        WebsocketCredentials creds = consoleResource.getWebsocketCredentials(userId, subscriptionId).getBody();
        System.out.println(creds.token());
        
        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.set("Origin", "https://panel-production-7c69.up.railway.app");

        session.getAttributes().put("token", creds.token());
        
        WebSocketSession pteroSession = client.doHandshake(
            new ConsoleWebSocketHandler(session), 
            headers, 
            URI.create(creds.socket())
        ).get();

        session.getAttributes().put("pteroSession", pteroSession);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        WebSocketSession pteroSession = (WebSocketSession) session.getAttributes().get("pteroSession");
        if (pteroSession != null && pteroSession.isOpen()) {
            pteroSession.sendMessage(message);
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        WebSocketSession pteroSession = (WebSocketSession) session.getAttributes().get("pteroSession");
        if (pteroSession != null && pteroSession.isOpen()) {
            pteroSession.close();
        }
    }
    
    private String extractSubscriptionId(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        if (parts.length >= 6) {
            return parts[5]; // subscriptionId is at index 5
        }
        throw new IllegalArgumentException("invalid websocket path: " + path);
    }

    private String extractUserId(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        if (parts.length >= 4) {
            return parts[3]; // userId is at index 3
        }
        throw new IllegalArgumentException("invalid websocket path: " + path);
    }
}
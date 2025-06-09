package com.mc_host.api.service.panel.websocket;

import org.springframework.lang.NonNull;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class ConsoleWebSocketHandler extends TextWebSocketHandler {
    private final WebSocketSession browserSession;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public ConsoleWebSocketHandler(WebSocketSession browserSession) {
        this.browserSession = browserSession;
    }
    
    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        if (browserSession.isOpen()) {
            browserSession.sendMessage(message);
        }
    }
    
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        String token = (String) browserSession.getAttributes().get("token");
        
        if (token != null) {
            Map<String, Object> authMessage = Map.of(
                "event", "auth",
                "args", new String[]{token}
            );
            
            String authJson = objectMapper.writeValueAsString(authMessage);
            session.sendMessage(new TextMessage(authJson));
        }
    }
}
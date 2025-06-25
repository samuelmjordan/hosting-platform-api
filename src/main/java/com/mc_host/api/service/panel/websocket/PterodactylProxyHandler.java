package com.mc_host.api.service.panel.websocket;

import com.mc_host.api.configuration.PterodactylConfiguration;
import com.mc_host.api.controller.api.subscriptions.panel.ConsoleController;
import com.mc_host.api.model.resource.pterodactyl.panel.WebsocketCredentials;
import com.mc_host.api.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@RequiredArgsConstructor
public class PterodactylProxyHandler extends TextWebSocketHandler {
    private static final Logger LOGGER = Logger.getLogger(PterodactylProxyHandler.class.getName());
    private static final String PTERODACTYL_SESSION = "PTERODACTYL_SESSION";
    private static final String USER_ID = "USER_ID";
    private static final String SUBSCRIPTION_ID = "SUBSCRIPTION_ID";

    private final ConsoleController consoleController;
    private final PterodactylConfiguration pterodactylConfiguration;
    private final SubscriptionRepository subscriptionRepository;
    private final JwtDecoder jwtDecoder;

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        try {
            String token = extractBearerToken(session);
            Jwt jwt = jwtDecoder.decode(token);
            String userId = jwt.getSubject(); // got the userId right here

            String subscriptionId = extractSubscriptionId(session);
            validateSubscriptionOwnership(userId, subscriptionId);

            LOGGER.info("establishing pterodactyl connection for user %s subscription %s".formatted(userId, subscriptionId));

            session.getAttributes().put(USER_ID, userId);
            session.getAttributes().put(SUBSCRIPTION_ID, subscriptionId);

            try {
                WebsocketCredentials creds = consoleController.getWebsocketCredentials(subscriptionId).getBody();

                if (creds == null) {
                    LOGGER.log(Level.SEVERE, "received null credentials for user %s subscription %s".formatted(userId, subscriptionId));
                    session.close(CloseStatus.SERVER_ERROR);
                    return;
                }

                StandardWebSocketClient client = new StandardWebSocketClient();
                WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
                headers.set("Origin", pterodactylConfiguration.getApiBase());

                session.getAttributes().put("token", creds.token());

                WebSocketSession pteroSession = client.doHandshake(
                    new ConsoleWebSocketHandler(session, consoleController, subscriptionId),
                    headers,
                    URI.create(creds.socket())
                ).get();

                session.getAttributes().put(PTERODACTYL_SESSION, pteroSession);
                LOGGER.log(Level.INFO, "successfully connected to pterodactyl for user %s subscription %s".formatted(userId, subscriptionId));

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "failed to connect to pterodactyl for user %s subscription %s".formatted(userId, subscriptionId), e);
                session.close(CloseStatus.SERVER_ERROR);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "authentication failed for websocket connection", e);
            session.close(CloseStatus.SERVER_ERROR);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        WebSocketSession pteroSession = (WebSocketSession) session.getAttributes().get(PTERODACTYL_SESSION);
        if (pteroSession != null && pteroSession.isOpen()) {
            pteroSession.sendMessage(message);
        } else {
            LOGGER.log(Level.WARNING, "attempted to send message but pterodactyl session is closed");
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        String userId = (String) session.getAttributes().get(USER_ID);
        String subscriptionId = (String) session.getAttributes().get(SUBSCRIPTION_ID);

        LOGGER.info("browser connection closed for user %s subscription %s, status: %s".formatted(userId, subscriptionId, status));

        WebSocketSession pteroSession = (WebSocketSession) session.getAttributes().get(PTERODACTYL_SESSION);
        if (pteroSession != null && pteroSession.isOpen()) {
            pteroSession.close(CloseStatus.NORMAL);
            LOGGER.info("closed pterodactyl connection for user %s subscription %s".formatted(userId, subscriptionId));
        }
    }

    private String extractSubscriptionId(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        if (parts.length >= 5) {
            return parts[4];
        }
        throw new IllegalArgumentException("invalid websocket path: " + path);
    }

    private String extractBearerToken(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    return URLDecoder.decode(param.substring(6), StandardCharsets.UTF_8);
                }
            }
        }
        throw new IllegalArgumentException("missing token parameter");
    }

    private void validateSubscriptionOwnership(String userId, String subscriptionId) {
        String subscriptionUserId = subscriptionRepository.selectSubscriptionOwnerUserId(subscriptionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Subscription %s not found".formatted(subscriptionId)));

        if (!userId.equals(subscriptionUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "User %s not authorised for subscription %s".formatted(userId, subscriptionId));
        }

        LOGGER.info("User %s authorised for subscription %s".formatted(userId, subscriptionId));
    }
}
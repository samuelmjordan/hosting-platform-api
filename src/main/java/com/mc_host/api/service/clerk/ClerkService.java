package com.mc_host.api.service.clerk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.configuration.ClerkConfiguration;
import com.mc_host.api.controller.webhook.ClerkWebhookController;
import com.mc_host.api.model.user.ClerkUserEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class ClerkService implements ClerkWebhookController {
    private static final Logger LOGGER = Logger.getLogger(ClerkService.class.getName());

    private final ClerkConfiguration clerkConfiguration;
    private final ClerkEventProcessor clerkEventProcessor;
    private final Executor virtualThreadExecutor;

    @Override
    public ResponseEntity<String> handleClerkWebhook(String payload, String svixId, String svixTimestamp, String svixSignature) {
        try {
            if(!verifySignature(payload, svixId, svixTimestamp, svixSignature)) {
                return ResponseEntity.status(HttpStatusCode.valueOf(403)).build();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode event = mapper.readTree(payload);
            String eventType = event.get("type").asText();

            if(!clerkConfiguration.isValidEventType(eventType)) {
                LOGGER.info("Invalid clerk webhook type: " + eventType);
                return ResponseEntity.ok().build();
            }

            ClerkUserEvent clerkUserEvent = new ClerkUserEvent(
                eventType,
                event.get("data").get("id").asText()
            );

            virtualThreadExecutor.execute(() -> clerkEventProcessor.processEvent(clerkUserEvent));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        
        LOGGER.info("Clerk webhook processed");
        return ResponseEntity.ok("webhook processed");
    }
    
    private Boolean verifySignature(String payload, String svixId, String svixTimestamp, String svixSignature) {
        try {
            String signedPayload = String.join(".", svixId, svixTimestamp, payload);
            String secret = clerkConfiguration.getSigningKey().split("_")[1];

            byte[] secretBytes = Base64.getDecoder().decode(secret);
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(signedPayload.getBytes());

            String calculatedSignature = Base64.getEncoder().encodeToString(hash);

            String[] svixSignatures = svixSignature.split(" ");
            for (String sig : svixSignatures) {
                if (sig.contains(",")) {
                    String actualSig = sig.substring(sig.indexOf(",") + 1);
                    if (MessageDigest.isEqual(calculatedSignature.getBytes(), actualSig.getBytes())) {
                        return true;
                    }
                }
            }
            
            return false;
        } catch (Exception e) {
            throw new RuntimeException("failed to compute signature", e);
        }
    }
    
}

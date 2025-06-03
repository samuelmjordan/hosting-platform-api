package com.mc_host.api.service.clerk;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.configuration.ClerkConfiguration;
import com.mc_host.api.controller.ClerkResource;

@Service
public class ClerkService implements ClerkResource {

    private static final Logger LOGGER = Logger.getLogger(ClerkService.class.getName());

    private ClerkConfiguration clerkConfiguration;

    public ClerkService(
        ClerkConfiguration clerkConfiguration
    ) {
        this.clerkConfiguration = clerkConfiguration;
    }

    @Override
    public ResponseEntity<String> handleClerkWebhook(String payload, String svixId, String svixTimestamp, String svixSignature) {
        try {
            if(!verifySignature(payload, svixId, svixTimestamp, svixSignature)) {
                return ResponseEntity.status(HttpStatusCode.valueOf(403)).build();
            }

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

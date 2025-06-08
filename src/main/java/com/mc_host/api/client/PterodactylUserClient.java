package com.mc_host.api.client;

import java.net.http.HttpClient;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.configuration.PterodactylConfiguration;
import com.mc_host.api.model.resource.pterodactyl.PowerState;

@Service
public class PterodactylUserClient extends BaseApiClient {

    private final PterodactylConfiguration config;

    PterodactylUserClient(
        PterodactylConfiguration config,
        HttpClient httpClient,
        ObjectMapper objectMapper
    ) {
        super(httpClient, objectMapper);
        this.config = config;
    }

    @Override
    protected String getApiBase() {
        return config.getApiBase();
    }

    @Override
    protected String getAuthorizationHeader() {
        return "Bearer " + config.getClientApiToken();
    }

    // SERVERS
    public void setPowerState(String serverUid, PowerState state) {
        var action = Map.of("signal", state.toString());
        sendRequest("POST", "/api/client/servers/" + serverUid + "/power", action);
    }
    
    public void acceptMinecraftEula(String serverUid) {
        sendRequest("POST", "/api/client/servers/" + serverUid + "/files/write?file=eula.txt", "eula=true");
    }

    public ServerStatus getServerStatus(String serverUid) {
        var response = sendRequest("GET", "/api/client/servers/" + serverUid + "/resources");
        
        try {
            @SuppressWarnings("unchecked")
            var responseMap = (Map<String, Object>) objectMapper.readValue(response, Map.class);
            @SuppressWarnings("unchecked")
            var attributes = (Map<String, Object>) responseMap.get("attributes");
            var status = (String) attributes.get("current_state");
            
            return ServerStatus.valueOf(status.toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException("failed to parse server status", e);
        }
    }

    public enum ServerStatus {
        RUNNING, STARTING, STOPPING, STOPPED, OFFLINE
    }

    public record PterodactylUserResponse(UserAttributes attributes) {}
    public record UserAttributes(
        Long id,
        String email,
        String username,
        String first_name,
        String last_name,
        String language,
        Boolean root_admin,
        Boolean use_totp,
        String created_at,
        String updated_at
    ) {}
}
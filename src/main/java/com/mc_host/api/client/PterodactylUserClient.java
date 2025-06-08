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
        var resources = getServerResources(serverUid);
        return ServerStatus.valueOf(resources.attributes().current_state().toUpperCase());
    }

    public ServerResourcesResponse getServerResources(String serverUid) {
        var response = sendRequest("GET", "/api/client/servers/" + serverUid + "/resources");
        return deserialize(response, ServerResourcesResponse.class);
    }

    public WebsocketCredentialsResponse getWebsocketCredentials(String serverUid) {
        var response = sendRequest("GET", "/api/client/servers/" + serverUid + "/websocket");
        return deserialize(response, WebsocketCredentialsResponse.class);
    }

    public void sendConsoleCommand(String serverUid, String command) {
        var payload = Map.of("command", command);
        sendRequest("POST", "/api/client/servers/" + serverUid + "/command", payload);
    }

    private <T> T deserialize(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("failed to deserialize response", e);
        }
    }

    public enum ServerStatus {
        RUNNING, STARTING, STOPPING, STOPPED, OFFLINE
    }

    public record ServerResourcesResponse(ServerResourcesAttributes attributes) {}
    public record ServerResourcesAttributes(
        String current_state,
        boolean is_suspended,
        ResourceStats resources
    ) {}
    
    public record ResourceStats(
        long memory_bytes,
        long memory_limit_bytes,
        double cpu_absolute,
        long network_rx_bytes,
        long network_tx_bytes,
        long disk_bytes,
        String uptime
    ) {}

    public record WebsocketCredentialsResponse(WebsocketData data) {}
    public record WebsocketData(
        String token,
        String socket
    ) {}

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
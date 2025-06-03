package com.mc_host.api.client;

import java.net.http.HttpClient;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.configuration.PterodactylConfiguration;
import com.mc_host.api.model.resource.pterodactyl.response.PaginatedResponse;

@Service
public class PterodactylUserClient extends BaseApiClient {

    private final PterodactylConfiguration pterodactylConfiguration;

    PterodactylUserClient(
        PterodactylConfiguration pterodactylConfiguration,
        HttpClient httpClient,
        ObjectMapper objectMapper
    ) {
        super(httpClient, objectMapper);
        this.pterodactylConfiguration = pterodactylConfiguration;
    }

    @Override
    protected String getApiBase() {
        return pterodactylConfiguration.getApiBase();
    }

    @Override
    protected String getAuthorizationHeader() {
        return "Bearer " + pterodactylConfiguration.getClientApiToken();
    }

    // SERVERS
    public void setPowerState(String serverUid, PowerState state) throws Exception {
        var action = Map.of("signal", state.toString().toLowerCase());
        sendRequest("POST", "/api/client/servers/" + serverUid + "/power", action);
    }
    
    public void acceptMinecraftEula(String serverUid) throws Exception {
        String requestBody = "eula=true";
        sendRequest("POST", "/api/client/servers/" + serverUid + "/files/write?file=eula.txt", requestBody);
    }

    public ServerStatus getServerStatus(String serverUid) throws Exception {
        String response = sendRequest("GET", "/api/client/servers/" + serverUid + "/resources");
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        Map<String, Object> attributes = (Map<String, Object>) responseMap.get("attributes");
        String status = (String) attributes.get("current_state");
        
        return ServerStatus.valueOf(status.toUpperCase());
    }

    public enum PowerState {
        START, STOP, RESTART, KILL
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
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

    // USERS
    public PterodactylUserResponse createUser(String email, String firstName, String lastName, String username) throws Exception {
        var userRequest = Map.of(
            "email", email,
            "username", username,
            "first_name", firstName,
            "last_name", lastName,
            "password", java.util.UUID.randomUUID().toString(), // random pw since they'll use oauth anyway
            "root_admin", false
        );
        
        String response = sendRequest("POST", "/api/application/users", userRequest);
        return objectMapper.readValue(response, PterodactylUserResponse.class);
    }

    public PterodactylUserResponse getUser(Long userId) throws Exception {
        String response = sendRequest("GET", "/api/application/users/" + userId);
        return objectMapper.readValue(response, PterodactylUserResponse.class);
    }

    public PterodactylUserResponse getUserByEmail(String email) throws Exception {
        String response = sendRequest("GET", "/api/application/users?filter[email]=" + email);
        PaginatedResponse<PterodactylUserResponse> paginatedResponse = objectMapper.readValue(response,
            objectMapper.getTypeFactory().constructParametricType(
                PaginatedResponse.class, PterodactylUserResponse.class));
        
        if (paginatedResponse.data().isEmpty()) {
            throw new RuntimeException("User not found with email: " + email);
        }
        
        return paginatedResponse.data().get(0);
    }

    public void deleteUser(Long userId) throws Exception {
        sendRequest("DELETE", "/api/application/users/" + userId);
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
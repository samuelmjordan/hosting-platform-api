package com.mc_host.api.client;

import java.net.http.HttpClient;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.configuration.PterodactylConfiguration;

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

}
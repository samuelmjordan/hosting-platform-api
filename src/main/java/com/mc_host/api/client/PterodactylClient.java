package com.mc_host.api.client;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.configuration.PterodactylConfiguration;
import com.mc_host.api.model.pterodactyl.request.PterodactylCreateNodeRequest;
import com.mc_host.api.model.pterodactyl.response.PterodactylNodeResponse;

@Service
public class PterodactylClient extends BaseApiClient {
    private static final Logger LOGGER = Logger.getLogger(PterodactylClient.class.getName());

    private final PterodactylConfiguration pterodactylConfiguration;

    PterodactylClient(
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
        return "Bearer " + pterodactylConfiguration.getApiToken();
    }

    // SERVERS
    public ServerResponse getServer(String serverId) throws Exception {
        String response = sendRequest("GET", "/api/application/servers/" + serverId);
        return objectMapper.readValue(response, ServerResponse.class);
    }

    public List<ServerResponse> getAllServers() throws Exception {
        String response = sendRequest("GET", "/api/application/servers");
        PaginatedResponse<ServerResponse> paginatedResponse = objectMapper.readValue(response, 
            objectMapper.getTypeFactory().constructParametricType(
                PaginatedResponse.class, ServerResponse.class));
        return paginatedResponse.data;
    }

    public ServerResponse createServer(Map<String, Object> serverDetails) throws Exception {
        var response = sendRequest("POST", "/api/application/servers", serverDetails);
        return objectMapper.readValue(response, ServerResponse.class);
    }

    public void deleteServer(String serverId) throws Exception {
        sendRequest("DELETE", "/api/application/servers/" + serverId);
    }

    public void setPowerState(String serverId, PowerState state) throws Exception {
        var action = Map.of("signal", state.toString().toLowerCase());
        sendRequest("POST", "/api/client/servers/" + serverId + "/power", action);
    }

    // NODES
    public PterodactylNodeResponse createNode(PterodactylCreateNodeRequest nodeDetails) throws Exception {
        String response = sendRequest("POST", "/api/application/nodes", nodeDetails);
        return objectMapper.readValue(response, PterodactylNodeResponse.class);
    }
    
    public PterodactylNodeResponse getNode(String nodeId) throws Exception {
        String response = sendRequest("GET", "/api/application/nodes/" + nodeId);
        return objectMapper.readValue(response, PterodactylNodeResponse.class);
    }
    
    public List<PterodactylNodeResponse> getAllNodes() throws Exception {
        String response = sendRequest("GET", "/api/application/nodes");
        PaginatedResponse<PterodactylNodeResponse> paginatedResponse = objectMapper.readValue(response,
            objectMapper.getTypeFactory().constructParametricType(
                PaginatedResponse.class, PterodactylNodeResponse.class));
        return paginatedResponse.data;
    }
    
    public void deleteNode(String nodeId) throws Exception {
        sendRequest("DELETE", "/api/application/nodes/" + nodeId);
    }
    
    public PterodactylNodeResponse updateNode(String nodeId, PterodactylCreateNodeRequest nodeDetails) throws Exception {
        String response = sendRequest("PATCH", "/api/application/nodes/" + nodeId, nodeDetails);
        return objectMapper.readValue(response, PterodactylNodeResponse.class);
    }

    public enum PowerState {
        START, STOP, RESTART, KILL
    }
    public record ServerResponse(String id, ServerAttributes attributes) {}
    public record ServerAttributes(
        String name,
        String description,
        String uuid,
        boolean suspended,
        LimitsObject limits
    ) {}
    public record LimitsObject(int memory, int disk, int cpu) {}
    public record PaginatedResponse<T>(List<T> data, MetaData meta) {}
    public record MetaData(Pagination pagination) {}
    public record Pagination(
        int total,
        int count,
        int perPage,
        int currentPage,
        int totalPages
    ) {}
}
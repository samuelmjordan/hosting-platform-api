package com.mc_host.api.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.configuration.PterodactylConfiguration;
import com.mc_host.api.model.entity.node.pterodactyl_request.PterodactylCreateNodeRequest;
import com.mc_host.api.model.entity.node.pterodactyl_response.PterodactylNodeResponse;
import org.springframework.web.bind.annotation.PostMapping;

@Service
public class PterodactylClient {
    private static final Logger LOGGER = Logger.getLogger(PterodactylClient.class.getName());

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final PterodactylConfiguration pterodactylConfiguration;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    PterodactylClient(
        PterodactylConfiguration pterodactylConfiguration,
        HttpClient httpClient,
        ObjectMapper objectMapper
    ) {
        this.pterodactylConfiguration = pterodactylConfiguration;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
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
    @PostMapping("/api")
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


    // REQUESTS
    private String sendRequest(String method, String path) throws Exception {
        return sendRequest(method, path, null);
    }

    private String sendRequest(String method, String path, Object body) throws Exception {
        var builder = HttpRequest.newBuilder()
            .uri(URI.create(pterodactylConfiguration.getApiBase() + path))
            .header("Authorization", "Bearer " + pterodactylConfiguration.getApiToken())
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .timeout(REQUEST_TIMEOUT);

        var request = (switch (method) {
            case "GET" -> builder.GET();
            case "DELETE" -> builder.DELETE();
            case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(
                body != null ? objectMapper.writeValueAsString(body) : ""));
            case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(
                body != null ? objectMapper.writeValueAsString(body) : ""));
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }).build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            throw new RuntimeException("API error: " + response.statusCode() + " " + response.body());
        }

        return response.body();
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
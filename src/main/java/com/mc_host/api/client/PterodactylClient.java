package com.mc_host.api.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.configuration.PterodactylConfiguration;

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

    private String sendRequest(String method, String path) throws Exception {
        return sendRequest(method, path, null);
    }

    private String sendRequest(String method, String path, Object body) throws Exception {
        var builder = HttpRequest.newBuilder()
            .uri(URI.create(pterodactylConfiguration.getApiBase() + path))
            .header("Authorization", "Bearer " + pterodactylConfiguration.getApiToken())
            .header("Content-Type", "application/json")
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
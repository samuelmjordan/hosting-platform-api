package com.mc_host.api.client;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.configuration.HetznerConfiguration;
import com.mc_host.api.model.hetzner.HetznerServerResponse;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

@Service
public class HetznerClient {
    private static final Logger LOGGER = Logger.getLogger(HetznerClient.class.getName());

    private static final Duration POLL_INTERVAL = Duration.ofSeconds(5);
    private static final Duration MAX_WAIT_TIME = Duration.ofMinutes(5);

    private final HetznerConfiguration hetznerConfiguration;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HetznerClient(
        HetznerConfiguration hetznerConfiguration,
        HttpClient httpClient,
        ObjectMapper objectMapper
    ) {
        this.hetznerConfiguration = hetznerConfiguration;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public HetznerServerResponse createServer(String name, String serverType, String location, String image) throws Exception {
        var requestBody = Map.of(
            "name", name,
            "server_type", serverType,
            "location", location,
            "image", image,
            "ssh_keys", List.of("dev")
        );

        var response = sendRequest("POST", "/servers", requestBody);
        return objectMapper.readValue(response, HetznerServerResponse.class);
    }

    public void deleteServer(long serverId) throws Exception {
        sendRequest("DELETE", "/servers/" + serverId, null);
    }

    public HetznerServerResponse getServer(long serverId) throws Exception {
        var response = sendRequest("GET", "/servers/" + serverId, null);
        return objectMapper.readValue(response, HetznerServerResponse.class);
    }

    public void powerOn(long serverId) throws Exception {
        var action = Map.of("action", "poweron");
        sendRequest("POST", "/servers/" + serverId + "/actions", action);
    }

    public void powerOff(long serverId) throws Exception {
        var action = Map.of("action", "poweroff");
        sendRequest("POST", "/servers/" + serverId + "/actions", action);
    }

    public void reset(long serverId) throws Exception {
        var action = Map.of("action", "reset");
        sendRequest("POST", "/servers/" + serverId + "/actions", action);
    }

    public Boolean waitForServerStatus(String hetznerId, String expectedStatus) throws Exception {
    long startTime = System.currentTimeMillis();
    while (System.currentTimeMillis() - startTime < MAX_WAIT_TIME.toMillis()) {
        try {
            HetznerServerResponse response = getServer(Long.parseLong(hetznerId));
            if (response != null && expectedStatus.equals(response.server.status)) {
                return true;
            }
            Thread.sleep(POLL_INTERVAL.toMillis());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to poll server status: " + hetznerId, e);
            throw e;
        }
    }
    return false;
}


    private String sendRequest(String method, String path, Object body) throws Exception {
        var builder = HttpRequest.newBuilder()
            .uri(URI.create(hetznerConfiguration.getApiBase() + path))
            .header("Authorization", "Bearer " + hetznerConfiguration.getApiToken())
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(30));

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

}

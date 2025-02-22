package com.mc_host.api.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class BaseApiClient {
    private static final Logger LOGGER = Logger.getLogger(BaseApiClient.class.getName());
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    protected final HttpClient httpClient;
    protected final ObjectMapper objectMapper;

    protected BaseApiClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    protected abstract String getApiBase();
    protected abstract String getAuthorizationHeader();

    protected String sendRequest(String method, String path) throws Exception {
        return sendRequest(method, path, null);
    }

    protected String sendRequest(String method, String path, Object body) throws Exception {
        var builder = HttpRequest.newBuilder()
            .uri(URI.create(getApiBase() + path))
            .header("Authorization", getAuthorizationHeader())
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
}
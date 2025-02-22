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
import com.mc_host.api.configuration.CloudflareConfiguration;

@Service
public class CloudflareClient {
    private static final Logger LOGGER = Logger.getLogger(CloudflareClient.class.getName());
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final CloudflareConfiguration cloudflareConfiguration;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CloudflareClient(
        CloudflareConfiguration cloudflareConfiguration,
        HttpClient httpClient,
        ObjectMapper objectMapper
    ) {
        this.cloudflareConfiguration = cloudflareConfiguration;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public DNSRecord createSRVRecord(
        String zoneName,
        String service,
        String protocol,
        String target,
        int priority,
        int weight,
        int port
    ) throws Exception {
        // Format: _service._protocol.name
        String name = String.format("_%s._%s.%s", service, protocol, zoneName);
        
        var recordData = Map.of(
            "type", "SRV",
            "name", name,
            "data", Map.of(
                "service", service,
                "proto", protocol,
                "name", target,
                "priority", priority,
                "weight", weight,
                "port", port,
                "target", target
            ),
            "ttl", 3600
        );

        String zoneId = getZoneId(zoneName);
        String response = sendRequest(
            "POST", 
            "/zones/" + zoneId + "/dns_records",
            recordData
        );
        return objectMapper.readValue(response, DNSRecord.class);
    }

    public DNSRecord createARecord(
        String zoneName,
        String name,
        String ipAddress,
        boolean proxied
    ) throws Exception {
        var recordData = Map.of(
            "type", "A",
            "name", name,
            "content", ipAddress,
            "proxied", proxied,
            "ttl", 3600
        );

        String zoneId = getZoneId(zoneName);
        String response = sendRequest(
            "POST",
            "/zones/" + zoneId + "/dns_records",
            recordData
        );
        return objectMapper.readValue(response, DNSRecord.class);
    }

    public void deleteDNSRecord(String zoneName, String recordId) throws Exception {
        String zoneId = getZoneId(zoneName);
        sendRequest("DELETE", "/zones/" + zoneId + "/dns_records/" + recordId);
    }

    public List<DNSRecord> getDNSRecords(String zoneName) throws Exception {
        String zoneId = getZoneId(zoneName);
        String response = sendRequest("GET", "/zones/" + zoneId + "/dns_records");
        PaginatedResponse<DNSRecord> records = objectMapper.readValue(response,
            objectMapper.getTypeFactory().constructParametricType(
                PaginatedResponse.class, DNSRecord.class));
        return records.result;
    }

    private String getZoneId(String zoneName) throws Exception {
        String response = sendRequest(
            "GET",
            "/zones?name=" + zoneName
        );
        PaginatedResponse<Zone> zones = objectMapper.readValue(response,
            objectMapper.getTypeFactory().constructParametricType(
                PaginatedResponse.class, Zone.class));
        
        if (zones.result.isEmpty()) {
            throw new RuntimeException("Zone not found: " + zoneName);
        }
        return zones.result.get(0).id;
    }

    private String sendRequest(String method, String path) throws Exception {
        return sendRequest(method, path, null);
    }

    private String sendRequest(String method, String path, Object body) throws Exception {
        var builder = HttpRequest.newBuilder()
            .uri(URI.create(cloudflareConfiguration.getApiBase() + path))
            .header("Authorization", "Bearer " + cloudflareConfiguration.getApiToken())
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

    public record Zone(String id, String name) {}
    public record DNSRecord(
        String id,
        String type,
        String name,
        String content,
        boolean proxied,
        int ttl
    ) {}
    public record PaginatedResponse<T>(
        boolean success,
        List<T> result,
        ResultInfo result_info
    ) {}
    public record ResultInfo(
        int page,
        int per_page,
        int total_pages,
        int count,
        int total_count
    ) {}
}
package com.mc_host.api.client;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.configuration.CloudflareConfiguration;

@Service
public class CloudflareClient extends BaseApiClient{
    private static final Logger LOGGER = Logger.getLogger(CloudflareClient.class.getName());

    private final CloudflareConfiguration cloudflareConfiguration;

    public CloudflareClient(
        CloudflareConfiguration cloudflareConfiguration,
        HttpClient httpClient,
        ObjectMapper objectMapper
    ) {
        super(httpClient, objectMapper);
        this.cloudflareConfiguration = cloudflareConfiguration;
    }

    @Override
    protected String getApiBase() {
        return cloudflareConfiguration.getApiBase();
    }

    @Override
    protected String getAuthorizationHeader() {
        return "Bearer " + cloudflareConfiguration.getApiToken();
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
        SingleRecordResponse<DNSRecord> record = objectMapper.readValue(response, objectMapper.getTypeFactory().constructParametricType(SingleRecordResponse.class, DNSRecord.class));
        return record.result;
    }

    public DNSRecord createCNameRecord(
        String zoneName,
        String name,
        String target,
        boolean proxied
    ) throws Exception {
        var recordData = Map.of(
            "type", "CNAME",
            "name", name,
            "content", target,
            "proxied", proxied,
            "ttl", 3600
        );

        String zoneId = getZoneId(zoneName);
        String response = sendRequest(
            "POST",
            "/zones/" + zoneId + "/dns_records",
            recordData
        );
        SingleRecordResponse<DNSRecord> record = objectMapper.readValue(response, objectMapper.getTypeFactory().constructParametricType(SingleRecordResponse.class, DNSRecord.class));
        return record.result;
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

    public record Zone(String id, String name) {}
    public record DNSRecord(
        String id,
        String type,
        String zoneId,
        String zoneName,
        String name,
        String content,
        Boolean proxied,
        Integer ttl
    ) {}
    public record SingleRecordResponse<T>(
        boolean success,
        T result,
        List<String> errors,
        List<String> messages
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
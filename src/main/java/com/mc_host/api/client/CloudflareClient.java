package com.mc_host.api.client;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
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

    public DNSRecordResponse createARecord(
        String zoneId,
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

        String response = sendRequest(
            "POST",
            "/zones/" + zoneId + "/dns_records",
            recordData
        );
        SingleRecordResponse<DNSRecordResponse> record = objectMapper.readValue(response, objectMapper.getTypeFactory().constructParametricType(SingleRecordResponse.class, DNSRecordResponse.class));
        return record.result;
    }

    public DNSRecordResponse createCNameRecord(
        String zoneId,
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

        String response = sendRequest(
            "POST",
            "/zones/" + zoneId + "/dns_records",
            recordData
        );
        SingleRecordResponse<DNSRecordResponse> record = objectMapper.readValue(response, objectMapper.getTypeFactory().constructParametricType(SingleRecordResponse.class, DNSRecordResponse.class));
        return record.result;
    }

    public void deleteDNSRecord(String zoneId, String recordId) throws Exception {
        sendRequest("DELETE", "/zones/" + zoneId + "/dns_records/" + recordId);
    }

    public List<DNSRecordResponse> getDNSRecords(String zoneName) throws Exception {
        String zoneId = getZoneId(zoneName);
        String response = sendRequest("GET", "/zones/" + zoneId + "/dns_records");
        PaginatedResponse<DNSRecordResponse> records = objectMapper.readValue(response,
            objectMapper.getTypeFactory().constructParametricType(
                PaginatedResponse.class, DNSRecordResponse.class));
        return records.result;
    }

    public List<DNSRecordResponse> getAllCRecords(String zoneId) throws Exception {
        return getDNSRecordsForZoneIdAndType("CNAME", zoneId);
    }
    
    public List<DNSRecordResponse> getAllARecords(String zoneId) throws Exception {
        return getDNSRecordsForZoneIdAndType("A", zoneId);
    }
    
    public List<String> getAllZones() throws Exception {
        String response = sendRequest("GET", "/zones?per_page=50");
        PaginatedResponse<Zone> zones = objectMapper.readValue(response,
            objectMapper.getTypeFactory().constructParametricType(
                PaginatedResponse.class, Zone.class));
                
        // If there are more pages, fetch them
        List<Zone> allZones = new java.util.ArrayList<>(zones.result);
        int totalPages = zones.result_info.total_pages;
        
        for (int page = 2; page <= totalPages; page++) {
            String pageResponse = sendRequest("GET", "/zones?per_page=50&page=" + page);
            PaginatedResponse<Zone> pageZones = objectMapper.readValue(pageResponse,
                objectMapper.getTypeFactory().constructParametricType(
                    PaginatedResponse.class, Zone.class));
            allZones.addAll(pageZones.result);
        }
        
        return allZones.stream().map(Zone::id).toList();
    }
    
    private List<DNSRecordResponse> getDNSRecordsForZoneIdAndType(String recordType, String zoneId) throws Exception {
        String typeParam = recordType != null ? "&type=" + recordType : "";
        String response = sendRequest("GET", "/zones/" + zoneId + "/dns_records?per_page=100" + typeParam);
        PaginatedResponse<DNSRecordResponse> records = objectMapper.readValue(response,
            objectMapper.getTypeFactory().constructParametricType(
                PaginatedResponse.class, DNSRecordResponse.class));
                
        // If there are more pages, fetch them
        List<DNSRecordResponse> allRecords = new java.util.ArrayList<>(records.result);
        int totalPages = records.result_info.total_pages;
        
        for (int page = 2; page <= totalPages; page++) {
            String pageResponse = sendRequest("GET", "/zones/" + zoneId + "/dns_records?per_page=100&page=" + page + typeParam);
            PaginatedResponse<DNSRecordResponse> pageRecords = objectMapper.readValue(pageResponse,
                objectMapper.getTypeFactory().constructParametricType(
                    PaginatedResponse.class, DNSRecordResponse.class));
            allRecords.addAll(pageRecords.result);
        }
        
        return allRecords;
    }
    
    public String getZoneId(String zoneName) throws Exception {
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
    public record DNSRecordResponse(
        String id,
        String type,
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
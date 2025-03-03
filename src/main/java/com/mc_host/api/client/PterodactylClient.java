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
    public PterodactylServerResponse getServer(String serverId) throws Exception {
        String response = sendRequest("GET", "/api/application/servers/" + serverId);
        return objectMapper.readValue(response, PterodactylServerResponse.class);
    }

    public List<PterodactylServerResponse> getAllServers() throws Exception {
        String response = sendRequest("GET", "/api/application/servers");
        PaginatedResponse<PterodactylServerResponse> paginatedResponse = objectMapper.readValue(response, 
            objectMapper.getTypeFactory().constructParametricType(
                PaginatedResponse.class, PterodactylServerResponse.class));
        return paginatedResponse.data;
    }

    public PterodactylServerResponse createServer(Map<String, Object> serverDetails) throws Exception {
        var response = sendRequest("POST", "/api/application/servers", serverDetails);
        return objectMapper.readValue(response, PterodactylServerResponse.class);
    }

    public void deleteServer(Long serverId) throws Exception {
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
    
    public void deleteNode(Long nodeId) throws Exception {
        sendRequest("DELETE", "/api/application/nodes/" + nodeId);
    }
    
    public PterodactylNodeResponse updateNode(String nodeId, PterodactylCreateNodeRequest nodeDetails) throws Exception {
        String response = sendRequest("PATCH", "/api/application/nodes/" + nodeId, nodeDetails);
        return objectMapper.readValue(response, PterodactylNodeResponse.class);
    }

    public String getNodeConfiguration(Long nodeId) throws Exception { 
        return sendRequest("GET", "/api/application/nodes/" + nodeId + "/configuration");
    }

    // ALLOCATIONS
    public List<AllocationResponse> getNodeAllocations(Long nodeId) throws Exception {
        String response = sendRequest("GET", "/api/application/nodes/" + nodeId + "/allocations");
        PaginatedResponse<AllocationResponse> paginatedResponse = objectMapper.readValue(response,
            objectMapper.getTypeFactory().constructParametricType(
                PaginatedResponse.class, AllocationResponse.class));
        return paginatedResponse.data;
    }

    public AllocationResponse createAllocation(Long nodeId, String ip, Integer port, String alias) throws Exception {
        var allocationRequest = Map.of(
            "ip", ip,
            "ports", List.of(port.toString()),
            "alias", alias != null ? alias : ""
        );
        
        String response = sendRequest("POST", "/api/application/nodes/" + nodeId + "/allocations", allocationRequest);
        return objectMapper.readValue(response, AllocationResponse.class);
    }

    public void createMultipleAllocations(Long nodeId, String ip, List<Integer> ports, String alias) throws Exception {
        var portsStr = ports.stream().map(Object::toString).toList();
        var allocationRequest = Map.of(
            "ip", ip,
            "ports", portsStr,
            "alias", alias != null ? alias : ""
        );
        
        sendRequest("POST", "/api/application/nodes/" + nodeId + "/allocations", allocationRequest);
    }

    public void deleteAllocation(Long nodeId, Long allocationId) throws Exception {
        sendRequest("DELETE", "/api/application/nodes/" + nodeId + "/allocations/" + allocationId);
    }

    public List<AllocationResponse> getUnassignedAllocations(Long nodeId) throws Exception {
        List<AllocationResponse> allocations = getNodeAllocations(nodeId);
        return allocations.stream()
            .filter(a -> a.attributes().assigned() == false)
            .toList();
    }

    public record AllocationResponse(AllocationAttributes attributes) {}
    
    public record AllocationAttributes(
        Long id,
        String ip,
        Integer port,
        String alias,
        Boolean assigned,
        Long node_id
    ) {}

    public enum PowerState {
        START, STOP, RESTART, KILL
    }
    public record PterodactylServerResponse(ServerAttributes attributes) {}
    public record ServerAttributes(
        Long id, 
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
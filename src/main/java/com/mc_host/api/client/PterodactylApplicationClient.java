package com.mc_host.api.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.configuration.PterodactylConfiguration;
import com.mc_host.api.model.resource.pterodactyl.request.PterodactylCreateNodeRequest;
import com.mc_host.api.model.resource.pterodactyl.response.PaginatedResponse;
import com.mc_host.api.model.resource.pterodactyl.response.PterodactylNodeResponse;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

@Service
public class PterodactylApplicationClient extends BaseApiClient {

    private final PterodactylConfiguration config;

    PterodactylApplicationClient(
        PterodactylConfiguration config,
        HttpClient httpClient,
        ObjectMapper objectMapper
    ) {
        super(httpClient, objectMapper);
        this.config = config;
    }

    @Override
    protected String getApiBase() {
        return config.getApiBase();
    }

    @Override
    protected String getAuthorizationHeader() {
        return "Bearer " + config.getApiToken();
    }

    // SERVERS
    public PterodactylServerResponse getServer(Long serverId) {
        var response = sendRequest("GET", "/api/application/servers/" + serverId);
        return deserialize(response, PterodactylServerResponse.class);
    }

    public List<PterodactylServerResponse> getAllServers() {
        var response = sendRequest("GET", "/api/application/servers");
        var paginated = deserializePaginated(response, PterodactylServerResponse.class);
        return paginated.data();
    }

    public PterodactylServerResponse createServer(Map<String, Object> serverDetails) {
        var response = sendRequest("POST", "/api/application/servers", serverDetails);
        return deserialize(response, PterodactylServerResponse.class);
    }

    public void deleteServer(Long serverId) {
        sendRequest("DELETE", "/api/application/servers/" + serverId);
    }

    public void reinstallServer(Long serverId) {
        sendRequest("POST", "/api/application/servers/" + serverId + "/reinstall");
    }

    // NODES
    public PterodactylNodeResponse createNode(PterodactylCreateNodeRequest nodeDetails) {
        var response = sendRequest("POST", "/api/application/nodes", nodeDetails);
        return deserialize(response, PterodactylNodeResponse.class);
    }
    
    public PterodactylNodeResponse getNode(String nodeId) {
        var response = sendRequest("GET", "/api/application/nodes/" + nodeId);
        return deserialize(response, PterodactylNodeResponse.class);
    }
    
    public List<PterodactylNodeResponse> getAllNodes() {
        var response = sendRequest("GET", "/api/application/nodes");
        var paginated = deserializePaginated(response, PterodactylNodeResponse.class);
        return paginated.data();
    }
    
    public void deleteNode(Long nodeId) {
        sendRequest("DELETE", "/api/application/nodes/" + nodeId);
    }
    
    public PterodactylNodeResponse updateNode(String nodeId, PterodactylCreateNodeRequest nodeDetails) {
        var response = sendRequest("PATCH", "/api/application/nodes/" + nodeId, nodeDetails);
        return deserialize(response, PterodactylNodeResponse.class);
    }

    public String getNodeConfiguration(Long nodeId) { 
        return sendRequest("GET", "/api/application/nodes/" + nodeId + "/configuration");
    }

    // ALLOCATIONS
    public List<AllocationResponse> getNodeAllocations(Long nodeId) {
        var response = sendRequest("GET", "/api/application/nodes/" + nodeId + "/allocations");
        var paginated = deserializePaginated(response, AllocationResponse.class);
        return paginated.data();
    }

    public AllocationResponse createAllocation(Long nodeId, String ip, Integer port, String alias) {
        var request = Map.of(
            "ip", ip,
            "ports", List.of(port.toString()),
            "alias", alias != null ? alias : ""
        );
        
        var response = sendRequest("POST", "/api/application/nodes/" + nodeId + "/allocations", request);
        return deserialize(response, AllocationResponse.class);
    }

    public void createMultipleAllocations(Long nodeId, String ip, List<Integer> ports, String alias) {
        var portsStr = ports.stream().map(Object::toString).toList();
        var request = Map.of(
            "ip", ip,
            "ports", portsStr,
            "alias", alias != null ? alias : ""
        );
        
        sendRequest("POST", "/api/application/nodes/" + nodeId + "/allocations", request);
    }

    public void deleteAllocation(Long nodeId, Long allocationId) {
        sendRequest("DELETE", "/api/application/nodes/" + nodeId + "/allocations/" + allocationId);
    }

    // USERS
    public PterodactylUserResponse getUser(String userId) {
        var response = sendRequest("GET", "/api/application/users/" + userId);
        return deserialize(response, PterodactylUserResponse.class);
    }

    public List<PterodactylUserResponse> getAllUsers() {
        var response = sendRequest("GET", "/api/application/users");
        var paginated = deserializePaginated(response, PterodactylUserResponse.class);
        return paginated.data();
    }

    public PterodactylUserResponse createUser(PterodactylCreateUserRequest userDetails) {
        var response = sendRequest("POST", "/api/application/users", userDetails);
        return deserialize(response, PterodactylUserResponse.class);
    }

    public PterodactylUserResponse updateUser(String userId, PterodactylUpdateUserRequest userDetails) {
        var response = sendRequest("PATCH", "/api/application/users/" + userId, userDetails);
        return deserialize(response, PterodactylUserResponse.class);
    }

    public void deleteUser(String userId) {
        sendRequest("DELETE", "/api/application/users/" + userId);
    }

    public List<AllocationResponse> getUnassignedAllocations(Long nodeId) {
        var allocations = getNodeAllocations(nodeId);
        return allocations.stream()
            .filter(a -> !a.attributes().assigned())
            .toList();
    }

    //STARTUP
    public PterodactylServerResponse updateServerStartup(Long serverId, PterodactylUpdateStartupRequest startupDetails) {
        var response = sendRequest("PATCH", "/api/application/servers/" + serverId + "/startup", startupDetails);
        return deserialize(response, PterodactylServerResponse.class);
    }

    private <T> T deserialize(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("failed to deserialize response", e);
        }
    }

    private <T> PaginatedResponse<T> deserializePaginated(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, 
                objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, clazz));
        } catch (Exception e) {
            throw new RuntimeException("failed to deserialize paginated response", e);
        }
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

    public record PterodactylUpdateStartupRequest(
        String startup,
        Map<String, String> environment,
        Long egg,
        String image,
        Boolean skip_scripts
    ) {}

    public record PterodactylServerResponse(ServerAttributes attributes) {}

    public record ServerAttributes(
        Long id,
        String external_id,
        String uuid,
        String identifier,
        String name,
        String description,
        Boolean suspended,
        LimitsObject limits,
        FeatureLimitsObject feature_limits,
        Long user,
        Long node,
        Long allocation,
        Long nest,
        Long egg,
        ContainerObject container,
        String updated_at,
        String created_at
    ) {}

    public record LimitsObject(
        Integer memory,
        Integer swap,
        Integer disk,
        Integer io,
        Integer cpu,
        Integer threads
    ) {}

    public record FeatureLimitsObject(
        Integer databases,
        Integer allocations,
        Integer backups
    ) {}

    public record ContainerObject(
        String startup_command,
        String image,
        Boolean installed,
        Map<String, String> environment
    ) {}

    public record PterodactylUserResponse(UserAttributes attributes) {}

    public record UserAttributes(
        Long id,
        String external_id,
        String uuid,
        String username,
        String email,
        String first_name,
        String last_name,
        String language,
        Boolean root_admin,
        Boolean twofa, // the api calls it "2fa" but java identifiers can't start with numbers
        String created_at,
        String updated_at
    ) {}

    public record PterodactylCreateUserRequest(
        String email,
        String username,
        String first_name,
        String last_name,
        String language,
        String password
    ) {}

    public record PterodactylUpdateUserRequest(
        String email,
        String username,
        String first_name,
        String last_name,
        String language,
        String password
    ) {}
}
package com.mc_host.api.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.configuration.HetznerCloudConfiguration;
import com.mc_host.api.model.resource.hetzner.HetznerServerResponse;
import com.mc_host.api.model.resource.hetzner.HetznerServerResponse.Server;
import com.mc_host.api.model.resource.hetzner.HetznerServersResponse;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class HetznerCloudClient extends BaseApiClient{
    private static final Logger LOGGER = Logger.getLogger(HetznerCloudClient.class.getName());

    private static final Duration POLL_INTERVAL = Duration.ofSeconds(10);
    private static final Duration MAX_WAIT_TIME = Duration.ofMinutes(2);

    private final HetznerCloudConfiguration hetznerCloudConfiguration;

    public HetznerCloudClient(
        HetznerCloudConfiguration hetznerCloudConfiguration,
        HttpClient httpClient,
        ObjectMapper objectMapper
    ) {
        super(httpClient, objectMapper);
        this.hetznerCloudConfiguration = hetznerCloudConfiguration;
    }

    
    @Override
    protected String getApiBase() {
        return hetznerCloudConfiguration.getApiBase();
    }

    @Override
    protected String getAuthorizationHeader() {
        return "Bearer " + hetznerCloudConfiguration.getApiToken();
    }

    public HetznerServerResponse createServer(String name, String serverType, String location, String image) throws Exception {
        var requestBody = Map.of(
            "name", name,
            "server_type", serverType,
            "location", location,
            "image", image,
            "ssh_keys", List.of("default")
        );

        String response = sendRequest("POST", "/servers", requestBody);
        return objectMapper.readValue(response, HetznerServerResponse.class);
    }

    public void deleteServer(long serverId) throws Exception {
        sendRequest("DELETE", "/servers/" + serverId);
    }

    public HetznerServerResponse getServer(long serverId) throws Exception {
        String response = sendRequest("GET", "/servers/" + serverId);
        return objectMapper.readValue(response, HetznerServerResponse.class);
    }

    public List<Server> getAllServers() throws Exception {
        String response = sendRequest("GET", "/servers");
        HetznerServersResponse hetznerResponse = objectMapper.readValue(response, HetznerServersResponse.class);
        return hetznerResponse.servers();
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

    public Boolean waitForServerStatus(Long hetznerId, String expectedStatus) throws Exception {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < MAX_WAIT_TIME.toMillis()) {
            try {
                Thread.sleep(POLL_INTERVAL.toMillis());
                HetznerServerResponse response = getServer(hetznerId);
                if (response != null && expectedStatus.equals(response.server.status)) {
                    return true;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to poll server status: " + hetznerId, e);
            }
        }
        return false;
    }

}

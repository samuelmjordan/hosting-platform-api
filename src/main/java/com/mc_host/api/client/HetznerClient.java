package com.mc_host.api.client;

import java.net.http.HttpClient;
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
public class HetznerClient extends BaseApiClient{
    private static final Logger LOGGER = Logger.getLogger(HetznerClient.class.getName());

    private static final Duration POLL_INTERVAL = Duration.ofSeconds(5);
    private static final Duration MAX_WAIT_TIME = Duration.ofMinutes(5);

    private final HetznerConfiguration hetznerConfiguration;

    public HetznerClient(
        HetznerConfiguration hetznerConfiguration,
        HttpClient httpClient,
        ObjectMapper objectMapper
    ) {
        super(httpClient, objectMapper);
        this.hetznerConfiguration = hetznerConfiguration;
    }

    
    @Override
    protected String getApiBase() {
        return hetznerConfiguration.getApiBase();
    }

    @Override
    protected String getAuthorizationHeader() {
        return "Bearer " + hetznerConfiguration.getApiToken();
    }

    public HetznerServerResponse createServer(String name, String serverType, String location, String image) throws Exception {
        var requestBody = Map.of(
            "name", name,
            "server_type", serverType,
            "location", location,
            "image", image,
            "ssh_keys", List.of("dev")
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
                HetznerServerResponse response = getServer(hetznerId);
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

}

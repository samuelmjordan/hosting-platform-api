package com.mc_host.api.client;

import java.net.http.HttpClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.configuration.HetznerRobotConfiguration;
import com.mc_host.api.model.hetzner.HetznerServerResponse;
import com.mc_host.api.model.hetzner.HetznerServerResponse.Server;
import com.mc_host.api.model.hetzner.HetznerServersResponse;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class HetznerRobotClient extends BaseApiClient{
    private final HetznerRobotConfiguration hetznerRobotConfiguration;

    public HetznerRobotClient(
        HetznerRobotConfiguration hetznerRobotConfiguration,
        HttpClient httpClient,
        ObjectMapper objectMapper
    ) {
        super(httpClient, objectMapper);
        this.hetznerRobotConfiguration = hetznerRobotConfiguration;
    }

    
    @Override
    protected String getApiBase() {
        return hetznerRobotConfiguration.getApiBase();
    }

    @Override
    protected String getAuthorizationHeader() {
        return "Bearer " + hetznerRobotConfiguration.getApiToken();
    }

    public HetznerServerResponse getServer(long serverId) throws Exception {
        String response = sendRequest("GET", "/server/" + serverId);
        return objectMapper.readValue(response, HetznerServerResponse.class);
    }

    public List<Server> getAllServers() throws Exception {
        String response = sendRequest("GET", "/server");
        HetznerServersResponse hetznerResponse = objectMapper.readValue(response, HetznerServersResponse.class);
        return hetznerResponse.servers();
    }

}

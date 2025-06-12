package com.mc_host.api.service.panel;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.mc_host.api.controller.panel.ConsoleResource;
import com.mc_host.api.model.panel.request.ServerCommandRequest;
import com.mc_host.api.model.resource.pterodactyl.PowerState;
import com.mc_host.api.model.resource.pterodactyl.PterodactylServer;
import com.mc_host.api.model.resource.pterodactyl.panel.PterodactylServerResources;
import com.mc_host.api.model.resource.pterodactyl.panel.WebsocketCredentials;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.resources.PterodactylService;
import com.mc_host.api.service.resources.v2.context.Context;

@Service
public class ConsoleService implements ConsoleResource {

    private final ServerExecutionContextRepository serverExecutionContextRepository;
    private final GameServerRepository gameServerRepository;
    private final PterodactylService pterodactylService;

    public ConsoleService(
        ServerExecutionContextRepository serverExecutionContextRepository,
        GameServerRepository gameServerRepository,
        PterodactylService pterodactylService
    ) {
        this.serverExecutionContextRepository = serverExecutionContextRepository;
        this.gameServerRepository = gameServerRepository;
        this.pterodactylService = pterodactylService;
    }

    @Override
    public ResponseEntity<WebsocketCredentials> getWebsocketCredentials(String userId, String subscriptionId) {
        String serverUid = getServerUid(subscriptionId);
        WebsocketCredentials credentials = pterodactylService.getWebsocketCredentials(serverUid);
        return ResponseEntity.ok(credentials);
    }

    @Override
    public ResponseEntity<PterodactylServerResources> getResourceUsage(String userId, String subscriptionId) {
        String serverUid = getServerUid(subscriptionId);
        PterodactylServerResources resources = pterodactylService.getServerResources(serverUid);
        return ResponseEntity.ok(resources);
    }

    private String getServerUid(String subscriptionId) {
        Long serverId = serverExecutionContextRepository.selectSubscription(subscriptionId)
            .map(Context::getPterodactylServerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404)));
        return gameServerRepository.selectPterodactylServer(serverId)
            .map(PterodactylServer::pterodactylServerUid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404)));
    }
    
}

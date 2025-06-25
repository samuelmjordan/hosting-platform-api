package com.mc_host.api.service.panel;

import com.mc_host.api.controller.api.subscriptions.panel.ConsoleController;
import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.resource.pterodactyl.PterodactylServer;
import com.mc_host.api.model.resource.pterodactyl.panel.PterodactylServerResources;
import com.mc_host.api.model.resource.pterodactyl.panel.WebsocketCredentials;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.resources.PterodactylService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ConsoleService implements ConsoleController {

    private final ServerExecutionContextRepository serverExecutionContextRepository;
    private final GameServerRepository gameServerRepository;
    private final PterodactylService pterodactylService;

    @Override
    public ResponseEntity<WebsocketCredentials> getWebsocketCredentials(String subscriptionId) {
        String serverUid = getServerUid(subscriptionId);
        WebsocketCredentials credentials = pterodactylService.getWebsocketCredentials(serverUid);
        return ResponseEntity.ok(credentials);
    }

    @Override
    public ResponseEntity<PterodactylServerResources> getResourceUsage(String subscriptionId) {
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

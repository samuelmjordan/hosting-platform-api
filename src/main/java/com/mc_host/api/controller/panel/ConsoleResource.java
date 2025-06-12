package com.mc_host.api.controller.panel;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mc_host.api.model.panel.request.ServerCommandRequest;
import com.mc_host.api.model.resource.pterodactyl.PowerState;
import com.mc_host.api.model.resource.pterodactyl.panel.PterodactylServerResources;
import com.mc_host.api.model.resource.pterodactyl.panel.WebsocketCredentials;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/panel/user/{userId}/subscription/{subscriptionId}/console")
public interface ConsoleResource {

    @GetMapping("/websocket")
    public ResponseEntity<WebsocketCredentials> getWebsocketCredentials(
        @PathVariable String userId,
        @PathVariable String subscriptionId
    );

    @GetMapping("/resources")
    public ResponseEntity<PterodactylServerResources> getResourceUsage(
        @PathVariable String userId,
        @PathVariable String subscriptionId
    );
}

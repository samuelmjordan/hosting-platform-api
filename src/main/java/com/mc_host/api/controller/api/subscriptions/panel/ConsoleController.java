package com.mc_host.api.controller.api.subscriptions.panel;

import com.mc_host.api.auth.ValidatedSubscription;
import com.mc_host.api.model.resource.pterodactyl.panel.PterodactylServerResources;
import com.mc_host.api.model.resource.pterodactyl.panel.WebsocketCredentials;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/panel/user/subscription/{subscriptionId}/console")
public interface ConsoleController {

    @GetMapping("websocket")
    public ResponseEntity<WebsocketCredentials> getWebsocketCredentials(
        @ValidatedSubscription String subscriptionId
    );

    @GetMapping("resources")
    public ResponseEntity<PterodactylServerResources> getResourceUsage(
        @ValidatedSubscription String subscriptionId
    );
}

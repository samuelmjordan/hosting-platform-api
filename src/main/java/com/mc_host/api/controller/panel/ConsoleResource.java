package com.mc_host.api.controller.panel;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mc_host.api.model.panel.request.ServerCommandRequest;
import com.mc_host.api.model.resource.pterodactyl.PowerState;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/panel/user/{userId}/subscription/{subscriptionId}")
public interface ConsoleResource {

    @GetMapping("websocket")
    public ResponseEntity<String> getWebsocketCredentials(
        @PathVariable String userId,
        @PathVariable String subscriptionId
    );

    @GetMapping("resources")
    public ResponseEntity<String> getResourceUsage(
        @PathVariable String userId,
        @PathVariable String subscriptionId
    );

    @PostMapping("command")
    public ResponseEntity<Void> sendCommand(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestBody ServerCommandRequest request 
    );

    @PostMapping("signal/{command}")
    public ResponseEntity<Void> changePowerState(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @PathVariable PowerState powerState 
    );
      
}

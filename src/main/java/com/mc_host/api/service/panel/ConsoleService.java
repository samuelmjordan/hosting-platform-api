package com.mc_host.api.service.panel;

import org.springframework.http.ResponseEntity;

import com.mc_host.api.controller.panel.ConsoleResource;
import com.mc_host.api.model.panel.request.ServerCommandRequest;
import com.mc_host.api.model.resource.pterodactyl.PowerState;

public class ConsoleService implements ConsoleResource {

    @Override
    public ResponseEntity<String> getWebsocketCredentials(String userId, String subscriptionId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getWebsocketCredentials'");
    }

    @Override
    public ResponseEntity<String> getResourceUsage(String userId, String subscriptionId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getResourceUsage'");
    }

    @Override
    public ResponseEntity<Void> sendCommand(String userId, String subscriptionId, ServerCommandRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendCommand'");
    }

    @Override
    public ResponseEntity<Void> changePowerState(String userId, String subscriptionId, PowerState powerState) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'changePowerState'");
    }
    
}

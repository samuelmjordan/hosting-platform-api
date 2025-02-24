package com.mc_host.api.exceptions.provisioning;

import com.mc_host.api.model.game_server.ProvisioningState;

public class PterodactylProvisioningException extends NodeProvisioningException {

    public PterodactylProvisioningException(
        String message,
        Throwable cause,
        String subscriptionId,
        String nodeId,
        Long hetznerNodeId,
        String pterodactylNodeId,
        ProvisioningState state 
    ) {
        super(
            message,
            cause,
            subscriptionId,
            nodeId,
            hetznerNodeId,
            pterodactylNodeId,
            state
        );
    }
}

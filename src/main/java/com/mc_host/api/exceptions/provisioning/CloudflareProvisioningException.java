package com.mc_host.api.exceptions.provisioning;

import com.mc_host.api.model.game_server.ProvisioningState;

public class CloudflareProvisioningException extends NodeProvisioningException {

    public CloudflareProvisioningException(
        String message,
        Throwable cause,
        String subscriptionId,
        String nodeId,
        Long hetznerNodeId,
        ProvisioningState state 
    ) {
        super(
            message,
            cause,
            subscriptionId,
            nodeId,
            hetznerNodeId,
            null,
            state
        );
    }
}

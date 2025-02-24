package com.mc_host.api.exceptions.provisioning;

import com.mc_host.api.model.game_server.ProvisioningState;

public class NodeProvisioningException extends RuntimeException {
    private final String subscriptionId;
    private final String nodeId;
    private final Long hetznerNodeId;
    private final String pterodactylNodeId;
    private final ProvisioningState state;
    
    protected NodeProvisioningException(
        String message,
        Throwable cause,
        String subscriptionId,
        String nodeId,
        Long hetznerNodeId,
        String pterodactylNodeId,
        ProvisioningState state
    ) {
        super(message, cause);
        this.subscriptionId = subscriptionId;
        this.nodeId = nodeId;
        this.hetznerNodeId = hetznerNodeId;
        this.pterodactylNodeId = pterodactylNodeId;
        this.state = state;
    }
}

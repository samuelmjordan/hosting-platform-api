package com.mc_host.api.exceptions.provisioning;

public class NodeProvisioningException extends RuntimeException {
    private final String nodeId;
    private final Long hetznerNodeId;
    private final Long pterodactylNodeId;
    
    protected NodeProvisioningException(
        String message,
        Throwable cause,
        String nodeId,
        Long hetznerNodeId,
        Long pterodactylNodeId
    ) {
        super(message, cause);
        this.nodeId = nodeId;
        this.hetznerNodeId = hetznerNodeId;
        this.pterodactylNodeId = pterodactylNodeId;
    }
}

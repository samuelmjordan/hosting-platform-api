package com.mc_host.api.exceptions.provisioning;

public class PterodactylProvisioningException extends NodeProvisioningException {

    public PterodactylProvisioningException(
        String message,
        Throwable cause,
        String nodeId,
        Long hetznerNodeId,
        String pterodactylNodeId
    ) {
        super(
            message,
            cause,
            nodeId,
            hetznerNodeId,
            pterodactylNodeId
        );
    }
}

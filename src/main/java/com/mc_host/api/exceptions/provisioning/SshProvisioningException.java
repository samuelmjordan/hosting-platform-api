package com.mc_host.api.exceptions.provisioning;

public class SshProvisioningException extends NodeProvisioningException {

    public SshProvisioningException(
        String message,
        Throwable cause,
        String nodeId,
        Long hetznerNodeId,
        Long pterodactylNodeId
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
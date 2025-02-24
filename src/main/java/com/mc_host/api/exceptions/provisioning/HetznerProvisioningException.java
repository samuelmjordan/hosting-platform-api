package com.mc_host.api.exceptions.provisioning;

public class HetznerProvisioningException extends NodeProvisioningException {

    public HetznerProvisioningException(
        String message,
        Throwable cause,
        String nodeId,
        Long hetznerNodeId
    ) {
        super(
            message,
            cause,
            nodeId,
            hetznerNodeId,
            null
        );
    }
}

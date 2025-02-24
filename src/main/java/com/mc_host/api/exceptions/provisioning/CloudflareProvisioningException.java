package com.mc_host.api.exceptions.provisioning;

public class CloudflareProvisioningException extends NodeProvisioningException {

    public CloudflareProvisioningException(
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

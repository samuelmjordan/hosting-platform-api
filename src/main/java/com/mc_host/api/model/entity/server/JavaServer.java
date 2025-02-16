package com.mc_host.api.model.entity.server;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.mc_host.api.service.product.JavaServerService;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class JavaServer implements ProvisionableServer {
    private static final Logger LOGGER = Logger.getLogger(JavaServerService.class.getName());

    private final String serverId;
    private final String subscriptionId;
    private final String planId;

    private String hetznerId;
    private String pterodactylId;
    @Builder.Default private ProvisioningState provisioningState = ProvisioningState.NEW;
    @Builder.Default private ProvisioningStatus provisioningStatus = ProvisioningStatus.OK;
    
    public JavaServer(
        String serverId,
        String subscriptionId,
        String planId,
        String hetznerId,
        String pterodactylId,
        ProvisioningState provisioningState,
        ProvisioningStatus provisioningStatus
    ) {
        this.serverId = serverId;
        this.hetznerId = hetznerId;
        this.pterodactylId = pterodactylId;
        this.subscriptionId = subscriptionId;
        this.planId = planId;
        this.provisioningState = provisioningState;
        this.provisioningStatus = provisioningStatus;

        if (serverId == null || subscriptionId == null || planId == null) {
            LOGGER.log(Level.SEVERE, String.format("Invalid java server object %s", this.toString()));
            throw new IllegalStateException("Required fields cannot be null");
        }
    }
}

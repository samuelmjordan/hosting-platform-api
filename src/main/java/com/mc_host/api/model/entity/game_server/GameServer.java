package com.mc_host.api.model.entity.game_server;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.mc_host.api.service.product.GameServerService;

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
public class GameServer implements ProvisionableServer {
    private static final Logger LOGGER = Logger.getLogger(GameServerService.class.getName());

    private final String serverId;
    private final String subscriptionId;
    private final String planId;

    private String nodeId;

    @Builder.Default private ProvisioningState provisioningState = ProvisioningState.NEW;
    @Builder.Default private Integer retryCount = 0;
    
    public GameServer(
        String serverId,
        String subscriptionId,
        String planId,
        String nodeId,
        ProvisioningState provisioningState,
        Integer retryCount
    ) {
        this.serverId = serverId;
        this.subscriptionId = subscriptionId;
        this.planId = planId;
        this.nodeId = nodeId;
        this.provisioningState = provisioningState;
        this.retryCount = retryCount;

        if (serverId == null || subscriptionId == null || planId == null) {
            LOGGER.log(Level.SEVERE, String.format("Invalid java server object %s", this.toString()));
            throw new IllegalStateException("Required fields cannot be null");
        }
    }
}

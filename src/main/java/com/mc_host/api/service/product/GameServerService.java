package com.mc_host.api.service.product;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.entity.SubscriptionEntity;
import com.mc_host.api.model.game_server.GameServer;
import com.mc_host.api.model.hetzner.HetznerRegion;
import com.mc_host.api.model.hetzner.HetznerServerType;
import com.mc_host.api.model.node.Node;
import com.mc_host.api.model.specification.SpecificationType;
import com.mc_host.api.persistence.GameServerRepository;
import com.mc_host.api.persistence.PlanRepository;
import com.mc_host.api.service.node.CloudNodeService;

@Service
public class GameServerService implements ProductService {
    private static final Logger LOGGER = Logger.getLogger(GameServerService.class.getName());

    private final GameServerRepository gameServerRepository;
    private final PlanRepository planRepository;
    private final CloudNodeService cloudNodeService;

    GameServerService(
        GameServerRepository gameServerRepository,
        PlanRepository planRepository,
        CloudNodeService cloudNodeService
    ) {
        this.gameServerRepository = gameServerRepository;
        this.planRepository = planRepository;
        this.cloudNodeService = cloudNodeService;
    }

    @Override
    public boolean isType(SpecificationType type) {
        return type.equals(SpecificationType.GAME_SERVER);
    }

    @Override
    public void handle(SubscriptionEntity subscription) {
        Optional<GameServer> javaServerEntity = gameServerRepository.selectJavaServerFromSubscription(subscription.subscriptionId());
        if (javaServerEntity.isEmpty() && subscription.status().equals("active")) {
            Node node = cloudNodeService.provisionCloudNode(HetznerRegion.NBG1, HetznerServerType.CAX11);

            String planId = planRepository.selectPlanIdFromPriceId(subscription.priceId())
                .orElseThrow(() -> new IllegalStateException("No spec associated with price " + subscription.priceId()));
            GameServer gameServer = GameServer.builder()
                .serverId(UUID.randomUUID().toString())
                .subscriptionId(subscription.subscriptionId())
                .planId(planId)
                .nodeId(node.getNodeId())
                .subdomain(node.getSubdomain()) // TODO: should make seperate server and node domains
                .build();
            gameServerRepository.insertNewJavaServer(gameServer);
        } else {
            //
        }
        return;
    }
    
}

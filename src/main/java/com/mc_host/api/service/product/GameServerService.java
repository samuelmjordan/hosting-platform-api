package com.mc_host.api.service.product;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.client.HetznerClient;
import com.mc_host.api.client.PterodactylClient;
import com.mc_host.api.model.entity.SubscriptionEntity;
import com.mc_host.api.model.entity.game_server.GameServer;
import com.mc_host.api.model.entity.game_server.ProvisioningState;
import com.mc_host.api.model.entity.node.Node;
import com.mc_host.api.model.entity.node.pterodactyl_request.PterodactylCreateNodeRequest;
import com.mc_host.api.model.entity.node.pterodactyl_response.PterodactylNodeResponse;
import com.mc_host.api.model.hetzner.HetznerRegion;
import com.mc_host.api.model.hetzner.HetznerServerType;
import com.mc_host.api.model.hetzner.HetznerServerResponse.Server;
import com.mc_host.api.model.specification.SpecificationType;
import com.mc_host.api.persistence.GameServerRepository;
import com.mc_host.api.persistence.NodeRepository;
import com.mc_host.api.persistence.PlanRepository;

@Service
public class GameServerService implements ProductService {
    private static final Logger LOGGER = Logger.getLogger(GameServerService.class.getName());
    private static final String SCHEME = "https";

    private final GameServerRepository gameServerRepository;
    private final NodeRepository nodeRepository;
    private final PlanRepository planRepository;
    private final WingsConfigService sshClient;
    private final HetznerClient hetznerClient;
    private final PterodactylClient pterodactylClient;

    GameServerService(
        GameServerRepository gameServerRepository,
        NodeRepository nodeRepository,
        PlanRepository planRepository,
        WingsConfigService sshClient,
        HetznerClient hetznerClient,
        PterodactylClient pterodactylClient
    ) {
        this.gameServerRepository = gameServerRepository;
        this.nodeRepository = nodeRepository;
        this.planRepository = planRepository;
        this.hetznerClient = hetznerClient;
        this.pterodactylClient = pterodactylClient;
        this.sshClient = sshClient;
    }

    @Override
    public boolean isType(SpecificationType type) {
        return type.equals(SpecificationType.GAME_SERVER);
    }

    @Override
    public void handle(SubscriptionEntity subscription) {
        Optional<GameServer> javaServerEntity = gameServerRepository.selectJavaServerFromSubscription(subscription.subscriptionId());
        if (javaServerEntity.isEmpty() && subscription.status().equals("active")) {
            this.createNodeAndServer(subscription);
        } else {
            //
        }
        return;
    }

    public void createNodeAndServer(SubscriptionEntity subscription) {
        LOGGER.log(Level.INFO, String.format("Creating node and game server from subscription %s", subscription.subscriptionId()));

        // Initialise game server and node objects   
        Node node = new Node();
        String planId = planRepository.selectPlanIdFromPriceId(subscription.priceId())
        .orElseThrow(() -> new IllegalStateException("No spec associated with price " + subscription.priceId()));
        GameServer gameServer = GameServer.builder()
            .serverId(UUID.randomUUID().toString())
            .subscriptionId(subscription.subscriptionId())
            .planId(planId)
            .nodeId(node.getNodeId())
            .build();
        gameServerRepository.insertNewJavaServer(gameServer);
        nodeRepository.insertNewNode(node);

        try {     
            // Create hetzner node
            gameServer.getProvisioningState().validateTransition(ProvisioningState.NODE_PROVISIONED);
            Server hetznerServer = hetznerClient.createServer(
                gameServer.getServerId(),
                HetznerServerType.CAX11.toString(),
                HetznerRegion.NBG1.toString(),
                "ubuntu-24.04"
            ).server;

            // If it  fails to come up, then teardown
            if (!hetznerClient.waitForServerStatus(hetznerServer.id, "running")) {
                hetznerClient.deleteServer(hetznerServer.id);
                throw new RuntimeException(String.format("Hetzner node %s for subscription %s failed to reach running state within timeout", node.getHetznerNodeId(), subscription.subscriptionId()));
            }

            // Update DB state
            node.setHetznerNodeId(hetznerServer.id);
            node.setIpv4(hetznerServer.public_net.ipv4.ip);
            node.setHetznerRegion(HetznerRegion.NBG1); // TODO: dont hardcode this!
            nodeRepository.updateNode(node);
            gameServer.transitionState(ProvisioningState.NODE_PROVISIONED);
            gameServerRepository.updateJavaServer(gameServer);

            // Install wings
            gameServer.getProvisioningState().validateTransition(ProvisioningState.NODE_CONFIGURED);
            sshClient.setupWings(node.getIpv4());

            // COnfigure pterodactyl node
            PterodactylCreateNodeRequest pterodactylNode = PterodactylCreateNodeRequest.builder()
                .name(node.getNodeId())
                .description(node.getNodeId())
                .locationId(HetznerRegion.NBG1.getPterodactylLocationId())
                .public_(true)
                .fqdn("temp.com")
                .scheme(SCHEME)
                .memory(1024)
                .memoryOverallocate(0)
                .disk(50000)
                .diskOverallocate(0)
                .uploadSize(100)
                .daemonSftp(2022)
                .daemonListen(8080)
                .build();
            PterodactylNodeResponse pterodactylResponse = pterodactylClient.createNode(pterodactylNode);
            gameServer.transitionState(ProvisioningState.NODE_CONFIGURED);
            gameServer.transitionState(ProvisioningState.READY);
            gameServer.resetRetryCount();
            gameServerRepository.updateJavaServer(gameServer);

            LOGGER.log(Level.INFO, String.format("Java server %s %s", gameServer.getServerId(), gameServer.getProvisioningState()));
        } catch(Exception e) {
            if (gameServer == null) {
                LOGGER.log(Level.SEVERE, String.format("Failure to create java server from subscription %s", subscription.subscriptionId()), e);
            }

            gameServer.incrementRetryCount();
            if (gameServer.getRetryCount() >= 3) {
                LOGGER.log(Level.SEVERE, String.format("Java server %s has attempted maximum retries. CRITICAL FAILURE. %s", gameServer.getServerId(), gameServer), e);
            }

            LOGGER.log(Level.SEVERE, String.format("Java server %s has failed. Attempt: %s", gameServer.getServerId(), gameServer.getRetryCount()), e);
            gameServer.incrementRetryCount();
            gameServerRepository.updateJavaServer(gameServer);
        }
    }
    
}

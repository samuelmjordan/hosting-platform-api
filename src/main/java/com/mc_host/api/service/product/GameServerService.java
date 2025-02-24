package com.mc_host.api.service.product;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.client.CloudflareClient;
import com.mc_host.api.client.HetznerClient;
import com.mc_host.api.client.PterodactylClient;
import com.mc_host.api.exceptions.provisioning.CloudflareProvisioningException;
import com.mc_host.api.exceptions.provisioning.HetznerProvisioningException;
import com.mc_host.api.exceptions.provisioning.NodeProvisioningException;
import com.mc_host.api.exceptions.provisioning.PterodactylProvisioningException;
import com.mc_host.api.exceptions.provisioning.SshProvisioningException;
import com.mc_host.api.model.entity.SubscriptionEntity;
import com.mc_host.api.model.game_server.GameServer;
import com.mc_host.api.model.game_server.ProvisioningState;
import com.mc_host.api.model.hetzner.HetznerRegion;
import com.mc_host.api.model.hetzner.HetznerServerType;
import com.mc_host.api.model.hetzner.HetznerServerResponse.Server;
import com.mc_host.api.model.node.Node;
import com.mc_host.api.model.pterodactyl.request.PterodactylCreateNodeRequest;
import com.mc_host.api.model.pterodactyl.response.PterodactylNodeResponse;
import com.mc_host.api.model.specification.SpecificationType;
import com.mc_host.api.persistence.GameServerRepository;
import com.mc_host.api.persistence.NodeRepository;
import com.mc_host.api.persistence.PlanRepository;

@Service
public class GameServerService implements ProductService {
    private static final Logger LOGGER = Logger.getLogger(GameServerService.class.getName());
    private static final String SCHEME = "https";
    private static final String DOMAIN = "samuelmjordan.dev";

    private final GameServerRepository gameServerRepository;
    private final NodeRepository nodeRepository;
    private final PlanRepository planRepository;
    private final WingsConfigService wingsConfigClient;
    private final HetznerClient hetznerClient;
    private final PterodactylClient pterodactylClient;
    private final CloudflareClient cloudflareClient;

    GameServerService(
        GameServerRepository gameServerRepository,
        NodeRepository nodeRepository,
        PlanRepository planRepository,
        WingsConfigService wingsConfigClient,
        HetznerClient hetznerClient,
        PterodactylClient pterodactylClient,
        CloudflareClient cloudflareClient
    ) {
        this.gameServerRepository = gameServerRepository;
        this.nodeRepository = nodeRepository;
        this.planRepository = planRepository;
        this.hetznerClient = hetznerClient;
        this.pterodactylClient = pterodactylClient;
        this.wingsConfigClient = wingsConfigClient;
        this.cloudflareClient = cloudflareClient;
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

        Node node = null;
        GameServer gameServer = null;
        try {
            // Initialise game server and node objects   
            node = new Node();
            String planId = planRepository.selectPlanIdFromPriceId(subscription.priceId())
            .orElseThrow(() -> new IllegalStateException("No spec associated with price " + subscription.priceId()));
            gameServer = GameServer.builder()
                .serverId(UUID.randomUUID().toString())
                .subscriptionId(subscription.subscriptionId())
                .planId(planId)
                .nodeId(node.getNodeId())
                .build();
            gameServerRepository.insertNewJavaServer(gameServer);
            nodeRepository.insertNewNode(node);

            // Provision and configure new node
            provisionNode(subscription, node, gameServer);

            LOGGER.log(Level.INFO, String.format("[node: %s] [hetznerNode: %s] [subscription: %s] Node is %s", node.getNodeId(), node.getHetznerNodeId(), subscription.subscriptionId(), gameServer.getProvisioningState()));

        } catch (NodeProvisioningException e) {
            gameServer.incrementRetryCount();
            if (gameServer.getRetryCount() <= 3) {
                LOGGER.log(Level.SEVERE, String.format("Java server %s has failed. Attempt: %s", gameServer.getServerId(), gameServer.getRetryCount()), e);
                gameServer.incrementRetryCount();
                gameServerRepository.updateJavaServer(gameServer);
    
                // retry()   
                return;
            }

            LOGGER.log(Level.SEVERE, String.format("Java server %s has attempted maximum retries. CRITICAL FAILURE. %s", gameServer.getServerId(), gameServer), e);
            // critical failure()   
        }
    }

    private void provisionNode(SubscriptionEntity subscription, Node node, GameServer gameServer) throws NodeProvisioningException {
        try {     
            provisionHetznerNode(subscription, node, gameServer);
            createDnsRecords(subscription, node, gameServer);
            configurePterodactylNode(subscription, node, gameServer);
            installWings(subscription, node, gameServer);

            LOGGER.log(Level.INFO, String.format("Java server %s %s", gameServer.getServerId(), gameServer.getProvisioningState()));
            return;
        } catch(HetznerProvisioningException e) {
            // cleanup
            throw e;
        } catch(CloudflareProvisioningException e) {
            // cleanup
            throw e;
        } catch(PterodactylProvisioningException e) {
            // cleanup
            throw e;
        } catch(SshProvisioningException e) {
            // cleanup
            throw e;
        }              
    };

    private void provisionHetznerNode(SubscriptionEntity subscription, Node node, GameServer gameServer) throws  HetznerProvisioningException {
        // Create hetzner node
        LOGGER.log(Level.INFO, String.format("[node: %s] [subscription: %s] Provisioning with hetzner", node.getNodeId(), subscription.subscriptionId()));
        Server hetznerServer = null;
        try {
            gameServer.getProvisioningState().validateTransition(ProvisioningState.NODE_PROVISIONED);
            hetznerServer = hetznerClient.createServer(
                "nodeId." + node.getNodeId(),
                HetznerServerType.CAX11.toString(),
                HetznerRegion.NBG1.toString(),
                "ubuntu-24.04"
            ).server;
        } catch (Exception e) {
            Long hetznerNodeId = hetznerServer == null ? null : hetznerServer.id;
            throw new HetznerProvisioningException(
                "Create hetzner node request failed",
                e,
                subscription.subscriptionId(),
                node.getNodeId(),
                hetznerNodeId,
                gameServer.getProvisioningState());
        }

        try {
            // If it  fails to come up, then teardown
            LOGGER.log(Level.INFO, String.format("[node: %s] [subscription: %s] Waiting for hetxner node to start", node.getNodeId(), subscription.subscriptionId()));
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
        } catch (Exception e) {
            throw new HetznerProvisioningException(
                "Time-out waiting for hetzner node to start",
                e,
                subscription.subscriptionId(),
                node.getNodeId(),
                node.getHetznerNodeId(),
                gameServer.getProvisioningState());
        }

    }

    private void createDnsRecords(SubscriptionEntity subscription, Node node, GameServer gameServer) throws  CloudflareProvisioningException {
        // Create DNS records with cloudflare
        LOGGER.log(Level.INFO, String.format("[node: %s] [hetznerNode: %s] [subscription: %s] Creating DNS records with cloudflare", node.getNodeId(), node.getHetznerNodeId(), subscription.subscriptionId()));
        try {
            gameServer.getProvisioningState().validateTransition(ProvisioningState.NODE_CONFIGURED);
            cloudflareClient.createARecord(DOMAIN, gameServer.getServerId(), node.getIpv4(), false);
            cloudflareClient.createSRVRecord(
                DOMAIN,
                gameServer.getServerId(),
                "tcp",
                String.join(".", gameServer.getServerId(), DOMAIN),
                0,
                0,
                25565);
        } catch (Exception e) {
            throw new CloudflareProvisioningException(
                "Failed to create cloudflare dns records",
                e,
                subscription.subscriptionId(),
                node.getNodeId(),
                node.getHetznerNodeId(),
                gameServer.getProvisioningState());
        }
    }

    private void configurePterodactylNode(SubscriptionEntity subscription, Node node, GameServer gameServer) throws  HetznerProvisioningException {
        // Configure pterodactyl node
        LOGGER.log(Level.INFO, String.format("[node: %s] [hetznerNode: %s] [subscription: %s] Registering node with pterodactyl", node.getNodeId(), node.getHetznerNodeId(), subscription.subscriptionId()));
        PterodactylCreateNodeRequest pterodactylNode = null;
        PterodactylNodeResponse pterodactylResponse = null;
        try {
            pterodactylNode = PterodactylCreateNodeRequest.builder()
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
            pterodactylResponse = pterodactylClient.createNode(pterodactylNode);
            node.setPterodactylNodeId(pterodactylResponse.attributes().uuid());
            nodeRepository.updateNode(node);
            gameServer.transitionState(ProvisioningState.NODE_CONFIGURED);
            gameServer.transitionState(ProvisioningState.READY);
            gameServer.resetRetryCount();
            gameServerRepository.updateJavaServer(gameServer);
        } catch (Exception e) {
            String pterodactylNodeId = pterodactylResponse == null ? null : pterodactylResponse.attributes().uuid();
            throw new PterodactylProvisioningException(
                "Failed to create cloudflare dns records",
                e,
                subscription.subscriptionId(),
                node.getNodeId(),
                node.getHetznerNodeId(),
                pterodactylNodeId,
                gameServer.getProvisioningState());
        }
    }

    private void installWings(SubscriptionEntity subscription, Node node, GameServer gameServer) throws  SshProvisioningException {
        // Install wings
        LOGGER.log(Level.INFO, String.format("[node: %s] [hetznerNode: %s] [subscription: %s] Installing wings via ssh", node.getNodeId(), node.getHetznerNodeId(), subscription.subscriptionId()));
        try {
            wingsConfigClient.setupWings(node.getIpv4());
        } catch (Exception e) {
            throw new SshProvisioningException(
                "Failed to install wings",
                e,
                subscription.subscriptionId(),
                node.getNodeId(),
                node.getHetznerNodeId(),
                node.getPterodactylNodeId(),
                gameServer.getProvisioningState());
        }
    }
    
}

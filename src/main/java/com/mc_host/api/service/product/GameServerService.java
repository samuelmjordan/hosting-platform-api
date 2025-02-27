package com.mc_host.api.service.product;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.client.CloudflareClient;
import com.mc_host.api.client.CloudflareClient.DNSRecord;
import com.mc_host.api.client.PterodactylClient;
import com.mc_host.api.client.PterodactylClient.AllocationAttributes;
import com.mc_host.api.client.PterodactylClient.AllocationResponse;
import com.mc_host.api.client.PterodactylClient.ServerResponse;
import com.mc_host.api.configuration.ApplicationConfiguration;
import com.mc_host.api.exceptions.provisioning.CloudflareProvisioningException;
import com.mc_host.api.model.entity.SubscriptionEntity;
import com.mc_host.api.model.game_server.GameServer;
import com.mc_host.api.model.hetzner.HetznerRegion;
import com.mc_host.api.model.hetzner.HetznerServerType;
import com.mc_host.api.model.node.Node;
import com.mc_host.api.model.pterodactyl.games.Egg;
import com.mc_host.api.model.pterodactyl.games.Nest;
import com.mc_host.api.model.specification.SpecificationType;
import com.mc_host.api.persistence.GameServerRepository;
import com.mc_host.api.persistence.NodeRepository;
import com.mc_host.api.persistence.PlanRepository;
import com.mc_host.api.service.node.CloudNodeService;

@Service
public class GameServerService implements ProductService {
    private static final Logger LOGGER = Logger.getLogger(GameServerService.class.getName());

    private final ApplicationConfiguration applicationConfiguration;
    private final NodeRepository nodeRepository;
    private final GameServerRepository gameServerRepository;
    private final PlanRepository planRepository;
    private final PterodactylClient pterodactylClient;
    private final CloudflareClient cloudflareClient;
    private final CloudNodeService cloudNodeService;

    GameServerService(
        ApplicationConfiguration applicationConfiguration,
        NodeRepository nodeRepository,
        GameServerRepository gameServerRepository,
        PlanRepository planRepository,
        PterodactylClient pterodactylClient,
        CloudflareClient cloudflareClient,
        CloudNodeService cloudNodeService
    ) {
        this.applicationConfiguration = applicationConfiguration;
        this.nodeRepository = nodeRepository;
        this.gameServerRepository = gameServerRepository;
        this.planRepository = planRepository;
        this.pterodactylClient = pterodactylClient;
        this.cloudflareClient = cloudflareClient;
        this.cloudNodeService = cloudNodeService;
    }

    @Override
    public boolean isType(SpecificationType type) {
        return type.equals(SpecificationType.GAME_SERVER);
    }

    @Override
    public void handle(SubscriptionEntity subscription) {
        Optional<GameServer> gameServerOptional = gameServerRepository.selectGameServerFromSubscription(subscription.subscriptionId());
        LOGGER.log(Level.INFO, String.format("Subscription %s server: %s", subscription.subscriptionId(), gameServerOptional.isPresent()));
        LOGGER.log(Level.INFO, String.format("Subscription %s status: %s", subscription.subscriptionId(), subscription.status()));

        if (gameServerOptional.isEmpty() && subscription.status().equals("active")) {
            LOGGER.log(Level.INFO, String.format("Init new subscription %s", subscription.subscriptionId()));
            Node node = cloudNodeService.provisionCloudNode(HetznerRegion.NBG1, HetznerServerType.CAX11);
            GameServer gameServer = createGameServerOnCloudNode(node, subscription);
            LOGGER.log(Level.INFO, String.format("Started server %s at %s.%s:%s", gameServer.getServerId(), gameServer.getRecordName(), gameServer.getZoneName(), gameServer.getPort()));

        } else if (gameServerOptional.isPresent() && !subscription.status().equals("active")) {
            LOGGER.log(Level.INFO, String.format("Cancelling server %s", gameServerOptional.get().getServerId()));
            Node node = nodeRepository.selectNode(gameServerOptional.get().getNodeId())
                .orElseThrow(() -> new IllegalStateException(String.format("Found server %s without a node", gameServerOptional.get().getServerId())));
            destroyGameServerOnCloudNode(node, gameServerOptional.get());
            LOGGER.log(Level.INFO, String.format("Destroyed server %s", gameServerOptional.get().getServerId()));

        } else {
            LOGGER.log(Level.INFO, String.format("Nothing to do for subscription %s", subscription.subscriptionId()));
        }
        return;
    }

    private GameServer createGameServerOnCloudNode(Node node, SubscriptionEntity subscription) {
        if (node.getDedicated()) {
            throw new IllegalStateException("Expected a cloud node, got a dedicated node");
        }

        String planId = planRepository.selectPlanIdFromPriceId(subscription.priceId())
            .orElseThrow(() -> new IllegalStateException("No spec associated with price " + subscription.priceId()));
        GameServer gameServer = new GameServer(
            UUID.randomUUID().toString(),
            subscription.subscriptionId(),
            planId,
            node.getNodeId()
        );
        gameServerRepository.insertNewJavaServer(gameServer);

        AllocationAttributes allocationAttributes = getAllocation(node);
        Long pterodactylServerId = createGameServer(node, gameServer, allocationAttributes);
        gameServer.setPterodactylServerId(pterodactylServerId);
        gameServerRepository.updateJavaServer(gameServer);

        DNSRecord cNameRecord = createCNameRecord(node, allocationAttributes);
        gameServer.setAllocationId(allocationAttributes.id());
        gameServer.setPort(allocationAttributes.port());
        gameServer.setCNameRecordId(cNameRecord.id());
        gameServer.setZoneName(applicationConfiguration.getDomain());
        gameServer.setRecordName(cNameRecord.name());
        gameServerRepository.updateJavaServer(gameServer);

        return gameServer;
    }

    private DNSRecord createCNameRecord(Node node, AllocationAttributes allocationAttributes) throws  CloudflareProvisioningException {
        LOGGER.log(Level.INFO, String.format("Creating CName record with cloudflare: %s.%s --> %s.%s", 
            "test", applicationConfiguration.getDomain(), node.getSubdomain(), applicationConfiguration.getDomain(), allocationAttributes.port()));
        try {
            return cloudflareClient.createCNameRecord(
                applicationConfiguration.getDomain(), 
                "test", 
                String.join(".", node.getSubdomain(), applicationConfiguration.getDomain()),
                false
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create cloudflare cname record", e);
        }
    }

    private Long createGameServer(Node node, GameServer gameServer, AllocationAttributes allocationAttributes) {
        try {
            Map<String, Object> serverDetails = Map.ofEntries(
                Map.entry("name", "Minecraft - " + gameServer.getServerId()),
                Map.entry("user", 1),
                Map.entry("egg", Egg.VANILLA_MINECRAFT.getId()),
                Map.entry("docker_image", "ghcr.io/pterodactyl/yolks:java_21"),
                Map.entry("startup", "java -Xms128M -Xmx{{SERVER_MEMORY}}M -jar server.jar"),
                Map.entry("environment", Map.of(
                    "SERVER_JARFILE", "server.jar",
                    "VANILLA_VERSION", "latest",
                    "BUILD_TYPE", "vanilla",
                    "GAMEMODE", "survival",
                    "DIFFICULTY", "normal",
                    "MAX_PLAYERS", "20"
                )),
                Map.entry("limits", Map.of(
                    "memory", 3584,
                    "swap", 0,
                    "disk", 15000,
                    "io", 500,
                    "cpu", 150
                )),
                Map.entry("feature_limits", Map.of(
                    "databases", 1,
                    "backups", 3
                )),
                Map.entry("allocation", Map.of(
                    "default", allocationAttributes.id()
                )),
                Map.entry("nest", Nest.MINECRAFT.getId()),
                Map.entry("external_id", gameServer.getServerId())
            );
            
            ServerResponse response = pterodactylClient.createServer(serverDetails);
            LOGGER.log(Level.INFO, "Created Minecraft server with Pterodactyl ID: " + response.attributes().id());
            
            return response.attributes().id();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create Minecraft server", e);
            throw new RuntimeException("Failed to create Minecraft server: " + e.getMessage(), e);
        }
    }

    private AllocationAttributes getAllocation(Node node) {
        try {
            List<AllocationResponse> unassignedAllocations = pterodactylClient.getUnassignedAllocations(node.getPterodactylNodeId());
            
            if (unassignedAllocations.isEmpty()) {
                LOGGER.log(Level.WARNING, "No unassigned allocations found on node " + node.getNodeId() + 
                    ". Creating a new allocation.");
                
                // Create a new allocation if none are available
                // Find a port that's not in use (starting from 25600 to avoid conflicts)
                Integer port = 25600;
                AllocationResponse response = pterodactylClient.createAllocation(
                    node.getPterodactylNodeId(),
                    node.getIpv4(),
                    port,
                    "Minecraft-" + port
                );
                return response.attributes();
            }
            
            // Return the first available allocation
            return unassignedAllocations.get(0).attributes();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get allocation for node " + node.getNodeId(), e);
            throw new RuntimeException("Failed to get server allocation: " + e.getMessage(), e);
        }
    }
    
    private void destroyGameServerOnCloudNode(Node node, GameServer gameServer) {
        if (node.getDedicated()) {
            throw new IllegalStateException("Expected a cloud node, got a dedicated node");
        }

        try {
            pterodactylClient.deleteServer(gameServer.getPterodactylServerId());
            cloudNodeService.destroyCloudNode(node.getNodeId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to destroy game server on cloud node", e);
        }
    }
}

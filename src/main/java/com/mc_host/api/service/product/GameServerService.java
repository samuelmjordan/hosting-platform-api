package com.mc_host.api.service.product;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.client.CloudflareClient;
import com.mc_host.api.client.CloudflareClient.DNSRecordResponse;
import com.mc_host.api.client.PterodactylClient;
import com.mc_host.api.client.PterodactylClient.AllocationAttributes;
import com.mc_host.api.client.PterodactylClient.AllocationResponse;
import com.mc_host.api.client.PterodactylClient.ServerResponse;
import com.mc_host.api.configuration.ApplicationConfiguration;
import com.mc_host.api.exceptions.provisioning.CloudflareProvisioningException;
import com.mc_host.api.model.entity.ContentSubscription;
import com.mc_host.api.model.game_server.DnsCNameRecord;
import com.mc_host.api.model.game_server.GameServer;
import com.mc_host.api.model.game_server.PterodactylServer;
import com.mc_host.api.model.hetzner.HetznerRegion;
import com.mc_host.api.model.hetzner.HetznerServerType;
import com.mc_host.api.model.node.DnsARecord;
import com.mc_host.api.model.node.Node;
import com.mc_host.api.model.node.PterodactylNode;
import com.mc_host.api.model.pterodactyl.games.Egg;
import com.mc_host.api.model.pterodactyl.games.Nest;
import com.mc_host.api.model.specification.SpecificationType;
import com.mc_host.api.persistence.GameServerRepository;
import com.mc_host.api.persistence.NodeRepository;
import com.mc_host.api.persistence.PlanRepository;
import com.mc_host.api.service.node.CloudNodeService;
import com.mc_host.api.util.Task;

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
    public void create(ContentSubscription subscription) {
        LOGGER.log(Level.INFO, String.format("Initialise resources for subscription %s", subscription.subscriptionId()));
        Node node = cloudNodeService.provisionCloudNode(HetznerRegion.NBG1, HetznerServerType.CAX11);
        GameServer gameServer = createGameServer(node, subscription);
        LOGGER.log(Level.INFO, String.format("Started server %s", gameServer.serverId()));
    }

    @Override
    public void delete(ContentSubscription subscription) {
        LOGGER.log(Level.INFO, String.format("Teardown resources for subscription %s", subscription.subscriptionId()));
        GameServer gameServer = gameServerRepository.selectGameServerFromSubscription(subscription.subscriptionId())
            .orElseThrow(() -> new IllegalStateException(String.format("Game server does not exist for subsccription %s",  subscription.subscriptionId())));

        Node node = nodeRepository.selectNode(gameServer.nodeId())
            .orElseThrow(() -> new IllegalStateException(String.format("Could not find node %s", gameServer.nodeId())));

        destroyGameServer(node, gameServer);
        LOGGER.log(Level.INFO, String.format("Destroyed server %s", gameServer.serverId()));
    }

    @Override
    public void update(ContentSubscription newSubscription, ContentSubscription oldSubsccription) {
        if(!newSubscription.subscriptionId().equals(oldSubsccription.subscriptionId())) {
            throw new IllegalStateException(String.format("Mismatched subscriptions for update: %s ans %s", newSubscription.subscriptionId(), oldSubsccription.subscriptionId()));
        }

        LOGGER.log(Level.INFO, String.format("Update resources for subscription %s", newSubscription.subscriptionId()));
        // update logic
    }

    private GameServer createGameServer(Node node, ContentSubscription subscription) {
        LOGGER.log(Level.INFO, String.format("[node %s] [subscription  %s] Creating game server", node.nodeId(), subscription.subscriptionId()));

        if (node.dedicated()) {
            throw new IllegalStateException("Expected a cloud node, got a dedicated node");
        }

        String planId = planRepository.selectPlanIdFromPriceId(subscription.priceId())
            .orElseThrow(() -> new IllegalStateException("No spec associated with price " + subscription.priceId()));
        GameServer gameServer = new GameServer(
            UUID.randomUUID().toString(),
            subscription.subscriptionId(),
            planId,
            node.nodeId()
        );
        gameServerRepository.insertGameServer(gameServer);

        AllocationAttributes allocationAttributes = getAllocation(node);
        Long pterodactylServerId = createGameServer(node, gameServer, allocationAttributes);
        PterodactylServer pterodactylServer = new PterodactylServer(gameServer.serverId(), pterodactylServerId, allocationAttributes.id(), allocationAttributes.port());
        gameServerRepository.insertPterodactylServer(pterodactylServer);

        DnsCNameRecord dnsCNameRecord = createCNameRecord(gameServer, node, allocationAttributes);
        gameServerRepository.insertDnsCNameRecord(dnsCNameRecord);

        return gameServer;
    }

    private DnsCNameRecord createCNameRecord(GameServer gameServer, Node node, AllocationAttributes allocationAttributes) throws  CloudflareProvisioningException {
        String subdomain = UUID.randomUUID().toString().replace("-", "");
        DnsARecord dnsARecord = nodeRepository.selectDnsARecord(node.nodeId())
            .orElseThrow(() -> new RuntimeException(String.format("Failed to find a record for node %s", node.nodeId())));
        LOGGER.log(Level.INFO, String.format("Creating CName record with cloudflare: %s.%s --> %s:%s", 
            subdomain, applicationConfiguration.getDomain(), dnsARecord.recordName(), allocationAttributes.port()));
        try {
            DNSRecordResponse dnsRecordResponse = cloudflareClient.createCNameRecord(
                dnsARecord.zoneId(), 
                subdomain, 
                dnsARecord.recordName(),
                false
            );
            return new DnsCNameRecord(
                gameServer.serverId(), 
                dnsRecordResponse.id(), 
                dnsARecord.zoneId(), 
                dnsARecord.zoneName(),
                dnsRecordResponse.name(),
                dnsRecordResponse.content()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create cloudflare cname record", e);
        }
    }

    private Long createGameServer(Node node, GameServer gameServer, AllocationAttributes allocationAttributes) {
        try {
            Map<String, Object> serverDetails = Map.ofEntries(
                Map.entry("name", "Minecraft - " + gameServer.serverId()),
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
                Map.entry("external_id", gameServer.serverId())
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
            PterodactylNode pterodactylNode = nodeRepository.selectPterodactylNode(node.nodeId())
                .orElseThrow(() -> new RuntimeException(String.format("Failed to find pterodactyl details for node %s", node.nodeId())));                    
            List<AllocationResponse> unassignedAllocations = pterodactylClient.getUnassignedAllocations(pterodactylNode.pterodactylNodeId());
            return unassignedAllocations.get(0).attributes();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get allocation for node " + node.nodeId(), e);
            throw new RuntimeException("Failed to get server allocation: " + e.getMessage(), e);
        }
    }

    private void destroyGameServer(Node node, GameServer gameServer) {
        if (node.dedicated()) {
            throw new IllegalStateException("Expected a cloud node, got a dedicated node");
        }

        Optional<PterodactylServer> pterodactylServer = gameServerRepository.selectPterodactylServer(gameServer.serverId());
        Optional<DnsCNameRecord> dnsCNameRecord = gameServerRepository.selectDnsCNameRecord(gameServer.serverId());
    
        CompletableFuture<Void> deletePterodactyl = Task.alwaysAttempt(
            String.format("Delete pterodactyl server for server %s", gameServer.serverId()), 
            () -> {
                if (pterodactylServer.isPresent()) {
                    pterodactylClient.deleteServer(pterodactylServer.get().pterodactylServerId());
                    gameServerRepository.deletePterodactylServer(pterodactylServer.get().serverId());
                }
            }
        );
    
        CompletableFuture<Void> deleteDns = Task.alwaysAttempt(
            String.format("Delete c name record for server %s", gameServer.serverId()), 
            () -> {
                if (dnsCNameRecord.isPresent()) {
                    cloudflareClient.deleteDNSRecord(dnsCNameRecord.get().zoneId(), dnsCNameRecord.get().cNameRecordId());
                    gameServerRepository.deleteDnsCNameRecord(dnsCNameRecord.get().serverId());
                }
            }
        );
    
        CompletableFuture<Void> deleteGameServer = Task.whenAllCompleteCritical(
            String.format("Delete game server %s", gameServer.serverId()), 
            () -> gameServerRepository.deleteGameServer(gameServer.serverId()),
            deletePterodactyl, deleteDns
        );

        CompletableFuture<Void> destroyNode = Task.whenAllCompleteNonCritical(
            String.format("Delete node %s", node.nodeId()), 
            () -> cloudNodeService.destroyCloudNode(node.nodeId()),
            deleteGameServer
        );

        Task.awaitCompletion(destroyNode);
    }
}

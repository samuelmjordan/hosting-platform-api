package com.mc_host.api.service.resources;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.client.PterodactylClient;
import com.mc_host.api.client.PterodactylClient.AllocationAttributes;
import com.mc_host.api.client.PterodactylClient.AllocationResponse;
import com.mc_host.api.client.PterodactylClient.PterodactylServerResponse;
import com.mc_host.api.exceptions.resources.PterodactylException;
import com.mc_host.api.model.game_server.GameServer;
import com.mc_host.api.model.game_server.PterodactylServer;
import com.mc_host.api.model.hetzner.HetznerRegion;
import com.mc_host.api.model.node.DnsARecord;
import com.mc_host.api.model.node.PterodactylNode;
import com.mc_host.api.model.pterodactyl.games.Egg;
import com.mc_host.api.model.pterodactyl.games.Nest;
import com.mc_host.api.model.pterodactyl.request.PterodactylCreateNodeRequest;
import com.mc_host.api.model.pterodactyl.response.PterodactylNodeResponse;
import com.mc_host.api.persistence.GameServerRepository;
import com.mc_host.api.persistence.NodeRepository;

@Service
public class PterodactylService {
    private static final Logger LOGGER = Logger.getLogger(PterodactylService.class.getName());

    private final PterodactylClient pterodactylClient;
    private final WingsService wingsService;
    private final NodeRepository nodeRepository;
    private final GameServerRepository gameServerRepository;

    public PterodactylService(
        PterodactylClient pterodactylClient,
        WingsService wingsService,
        NodeRepository nodeRepository,
        GameServerRepository gameServerRepository
    ) {
        this.pterodactylClient = pterodactylClient;
        this.wingsService = wingsService;
        this.nodeRepository = nodeRepository;
        this.gameServerRepository = gameServerRepository;
    }

    public PterodactylNode createNode(DnsARecord dnsARecord) {
        LOGGER.log(Level.INFO, String.format("[aRecordId: %s] Creating pterodactyl node", dnsARecord.aRecordId()));
        try {
            PterodactylCreateNodeRequest pterodactylNodeRequest = PterodactylCreateNodeRequest.builder()
                .name(dnsARecord.nodeId())
                .description(dnsARecord.nodeId())
                .locationId(HetznerRegion.NBG1.getPterodactylLocationId())
                .public_(true)
                .fqdn(dnsARecord.recordName())
                .scheme("https")
                .memory(1024)
                .memoryOverallocate(0)
                .disk(50000)
                .diskOverallocate(0)
                .uploadSize(100)
                .daemonSftp(2022)
                .daemonListen(8080)
                .build();
            PterodactylNodeResponse pterodactylNodeResponse = pterodactylClient.createNode(pterodactylNodeRequest);
            PterodactylNode pterodactylNode = new PterodactylNode(
                dnsARecord.nodeId(),
                pterodactylNodeResponse.attributes().id()
            );
            nodeRepository.insertPterodactylNode(pterodactylNode);
            LOGGER.log(Level.INFO, String.format("[gameServerId: %s] [pterodactylNodeId: %s] Created pterodactyl node", dnsARecord.aRecordId(), pterodactylNodeResponse.attributes().id()));
            return pterodactylNode;
        } catch (Exception e) {
            throw new PterodactylException(String.format("[aRecordId: %s] Error creating pterodactyl node", dnsARecord.aRecordId()), e);
        }
    }

    public void destroyNode(Long pterodactylNodeId) {
        LOGGER.log(Level.INFO, String.format("[pterodactylNodeId: %s] Deleting pterodactyl node", pterodactylNodeId));
        try {
            pterodactylClient.deleteNode(pterodactylNodeId);
            nodeRepository.deletePterodactylNode(pterodactylNodeId);
            LOGGER.log(Level.INFO, String.format("[pterodactylNodeId: %s] Deleted pterodactyl node", pterodactylNodeId));
        } catch (Exception e) {
            throw new PterodactylException(String.format("[pterodactylNodeId: %s] Error deleting pterodactyl node", pterodactylNodeId), e);
        }
    }

    public void destroyNodeWithGameServerId(String nodeId) {
        PterodactylNode pterodactylNode = nodeRepository.selectPterodactylNode(nodeId)
            .orElseThrow(() -> new IllegalStateException(String.format("[gameServerId: %s] No pterodactyl node associated with node", nodeId)));
        destroyNode(pterodactylNode.pterodactylNodeId());
    }

    public void configureNode(Long pterodactylNodeId, DnsARecord dnsARecord) {
        LOGGER.log(Level.INFO, String.format(String.format("[aRecordId: %s] Setting up wings", dnsARecord.aRecordId())));
        String wingsConfigJson = getNodeConfig(pterodactylNodeId);
        try {
            wingsService.setupWings(dnsARecord, wingsConfigJson);
            LOGGER.log(Level.INFO, String.format(String.format("[aRecordId: %s] Set up wings", dnsARecord.aRecordId())));
        } catch (Exception e) {
            throw new PterodactylException(String.format("[aRecordId: %s] Error setting up wings", dnsARecord.aRecordId()), e);
        }
    }

    private String getNodeConfig(Long pterodactylNodeId) {
        LOGGER.log(Level.INFO, String.format(String.format("[pterodactylNodeId: %s] Fetching wings config", pterodactylNodeId)));
        try {
            String nodeConfigJson = pterodactylClient.getNodeConfiguration(pterodactylNodeId);
            LOGGER.log(Level.INFO, String.format(String.format("[pterodactylNodeId: %s] Fetched wings config", pterodactylNodeId)));
            return nodeConfigJson;
        } catch (Exception e) {
            throw new PterodactylException(String.format("[pterodactylNodeId: %s] Error fetching wings config", pterodactylNodeId), e);
        }
    }

    public void createAllocation(Long pterodactylNodeId, String ipv4, Integer port) {
        LOGGER.log(Level.INFO, String.format(String.format("[pterodactylNodeId: %s] Creating pterodactyl node allocation", pterodactylNodeId)));
        try {
            pterodactylClient.createMultipleAllocations(
                pterodactylNodeId,
                ipv4,
                List.of(port),
                "Minecraft"
            );
            LOGGER.log(Level.INFO, String.format(String.format("[pterodactylNodeId: %s] Created pterodactyl node allocation", pterodactylNodeId)));
        } catch (Exception e) {
            throw new PterodactylException(String.format(String.format("[pterodactylNodeId: %s] Error creating pterodactyl node allocation", pterodactylNodeId)), e);
        }
    }

    public AllocationAttributes getAllocation(Long pterodactylNodeId) {
        LOGGER.log(Level.INFO, String.format(String.format("[pterodactylNodeId: %s] Fetching pterodactyl node allocation", pterodactylNodeId)));
        try {                
            List<AllocationResponse> unassignedAllocations = pterodactylClient.getUnassignedAllocations(pterodactylNodeId);
            LOGGER.log(Level.INFO, String.format(String.format("[pterodactylNodeId: %s] Fetched pterodactyl node allocation", pterodactylNodeId)));
            return unassignedAllocations.get(0).attributes();
        } catch (Exception e) {
            throw new PterodactylException(String.format("[pterodactylNodeId: %s] Error fetching pterodactyl node allocation", pterodactylNodeId), e);
        }
    }

    public PterodactylServer createServer(GameServer gameServer, AllocationAttributes allocationAttributes) {
        LOGGER.log(Level.INFO, String.format("[gameServerId: %s] Creating pterodactyl server", gameServer.serverId()));
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
            
            PterodactylServerResponse response = pterodactylClient.createServer(serverDetails);
            PterodactylServer pterodactylServer = new PterodactylServer(
                gameServer.serverId(), 
                response.attributes().id(), 
                allocationAttributes.id(), 
                allocationAttributes.port()
            );
            gameServerRepository.insertPterodactylServer(pterodactylServer);
            LOGGER.log(Level.INFO, String.format("[gameServerId: %s] [pterodactylServerId: %s] Created pterodactyl server", gameServer.serverId(), response.attributes().id()));
            return pterodactylServer;
        } catch (Exception e) {
            throw new PterodactylException(String.format("[gameServerId: %s] Error creating pterodactyl server", gameServer.serverId()), e);
        }
    }

    public void destroyServer(Long pterodactylServerId) {
        LOGGER.log(Level.INFO, String.format("[pterodactylServerId: %s] Deleting pterodactyl server", pterodactylServerId));
        try {
            pterodactylClient.deleteServer(pterodactylServerId);
            gameServerRepository.deletePterodactylServer(pterodactylServerId);
            LOGGER.log(Level.INFO, String.format("[pterodactylServerId: %s] Deleted pterodactyl server", pterodactylServerId));
        } catch (Exception e) {
            throw new PterodactylException(String.format("[pterodactylServerId: %s] Error deleting pterodactyl server", pterodactylServerId), e);
        }
    }

    public void destroyServerWithGameServerId(String gameServerId) {
        PterodactylServer pterodactylServer = gameServerRepository.selectPterodactylServer(gameServerId)
            .orElseThrow(() -> new IllegalStateException(String.format("[gameServerId: %s] No pterodactyl server associated with game server", gameServerId)));
        destroyServer(pterodactylServer.pterodactylServerId());
    }
    
}

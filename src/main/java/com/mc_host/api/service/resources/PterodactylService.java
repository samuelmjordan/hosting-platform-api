package com.mc_host.api.service.resources;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.client.PterodactylApplicationClient;
import com.mc_host.api.client.PterodactylUserClient;
import com.mc_host.api.client.PterodactylUserClient.PowerState;
import com.mc_host.api.client.PterodactylUserClient.ServerStatus;
import com.mc_host.api.client.PterodactylApplicationClient.AllocationAttributes;
import com.mc_host.api.client.PterodactylApplicationClient.AllocationResponse;
import com.mc_host.api.client.PterodactylApplicationClient.PterodactylServerResponse;
import com.mc_host.api.exceptions.resources.PterodactylException;
import com.mc_host.api.model.game_server.PterodactylServer;
import com.mc_host.api.model.hetzner.HetznerRegion;
import com.mc_host.api.model.node.DnsARecord;
import com.mc_host.api.model.node.PterodactylAllocation;
import com.mc_host.api.model.node.PterodactylNode;
import com.mc_host.api.model.pterodactyl.games.Egg;
import com.mc_host.api.model.pterodactyl.games.Nest;
import com.mc_host.api.model.pterodactyl.request.PterodactylCreateNodeRequest;
import com.mc_host.api.model.pterodactyl.response.PterodactylNodeResponse;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.util.PersistenceContext;

@Service
public class PterodactylService {
    private static final Logger LOGGER = Logger.getLogger(PterodactylService.class.getName());

    private final PterodactylApplicationClient pterodactylApplicationClient;
    private final PterodactylUserClient pterodactylUserClient;
    private final WingsService wingsService;
    private final NodeRepository nodeRepository;
    private final GameServerRepository gameServerRepository;
    private final PersistenceContext persistenceContext;

    public PterodactylService(
        PterodactylApplicationClient pterodactylApplicationClient,
        PterodactylUserClient pterodactylUserClient,
        WingsService wingsService,
        NodeRepository nodeRepository,
        GameServerRepository gameServerRepository,
        PersistenceContext persistenceContext
    ) {
        this.pterodactylApplicationClient = pterodactylApplicationClient;
        this.pterodactylUserClient = pterodactylUserClient;
        this.wingsService = wingsService;
        this.nodeRepository = nodeRepository;
        this.gameServerRepository = gameServerRepository;
        this.persistenceContext = persistenceContext;
    }

    public PterodactylNode createNode(DnsARecord dnsARecord) {
        LOGGER.log(Level.INFO, String.format("[aRecordId: %s] Creating pterodactyl node", dnsARecord.aRecordId()));
        try {
            PterodactylCreateNodeRequest pterodactylNodeRequest = PterodactylCreateNodeRequest.builder()
                .name(UUID.randomUUID().toString())
                .description(dnsARecord.subscriptionId())
                .locationId(HetznerRegion.NBG1.getPterodactylLocationId())
                .public_(true)
                .fqdn(dnsARecord.recordName())
                .scheme("https")
                .behindProxy(false)
                .memory(1024)
                .memoryOverallocate(0)
                .disk(50000)
                .diskOverallocate(0)
                .uploadSize(100)
                .daemonSftp(2022)
                .daemonListen(8080)
                .build();
            PterodactylNodeResponse pterodactylNodeResponse = pterodactylApplicationClient.createNode(pterodactylNodeRequest);
            PterodactylNode pterodactylNode = new PterodactylNode(
                dnsARecord.subscriptionId(),
                pterodactylNodeResponse.attributes().id()
            );
            LOGGER.log(Level.INFO, String.format("[gameServerId: %s] [pterodactylNodeId: %s] Created pterodactyl node", dnsARecord.aRecordId(), pterodactylNodeResponse.attributes().id()));
            return pterodactylNode;
        } catch (Exception e) {
            throw new PterodactylException(String.format("[aRecordId: %s] Error creating pterodactyl node", dnsARecord.aRecordId()), e);
        }
    }

    public void destroyNode(Long pterodactylNodeId) {
        LOGGER.log(Level.INFO, String.format("[pterodactylNodeId: %s] Deleting pterodactyl node", pterodactylNodeId));
        try {
            persistenceContext.inTransaction(() -> {
                nodeRepository.deletePterodactylNode(pterodactylNodeId);
                pterodactylApplicationClient.deleteNode(pterodactylNodeId);
            });
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
            String nodeConfigJson = pterodactylApplicationClient.getNodeConfiguration(pterodactylNodeId);
            LOGGER.log(Level.INFO, String.format(String.format("[pterodactylNodeId: %s] Fetched wings config", pterodactylNodeId)));
            return nodeConfigJson;
        } catch (Exception e) {
            throw new PterodactylException(String.format("[pterodactylNodeId: %s] Error fetching wings config", pterodactylNodeId), e);
        }
    }

    public void createAllocation(Long pterodactylNodeId, String ipv4, Integer port) {
        LOGGER.log(Level.INFO, String.format(String.format("[pterodactylNodeId: %s] Creating pterodactyl node allocation", pterodactylNodeId)));
        try {
            pterodactylApplicationClient.createMultipleAllocations(
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

    public PterodactylAllocation getAllocation(String subscriptionId, Long pterodactylNodeId) {
        LOGGER.log(Level.INFO, String.format(String.format("[pterodactylNodeId: %s] Fetching pterodactyl node allocation", pterodactylNodeId)));
        try {                
            List<AllocationResponse> unassignedAllocations = pterodactylApplicationClient.getUnassignedAllocations(pterodactylNodeId);
            LOGGER.log(Level.INFO, String.format(String.format("[pterodactylNodeId: %s] Fetched pterodactyl node allocation", pterodactylNodeId)));
            AllocationAttributes allocationAttributes = unassignedAllocations.get(0).attributes();
            return new PterodactylAllocation(
                subscriptionId,
                allocationAttributes.id(),
                allocationAttributes.ip(),
                allocationAttributes.port(),
                allocationAttributes.alias()
            );
        } catch (Exception e) {
            throw new PterodactylException(String.format("[pterodactylNodeId: %s] Error fetching pterodactyl node allocation", pterodactylNodeId), e);
        }
    }

    public PterodactylServer createServer(String subscriptionId, PterodactylAllocation allocation) {
        LOGGER.log(Level.INFO, String.format("[subscriptionId: %s] Creating pterodactyl server", subscriptionId));
        try {
            Map<String, Object> serverDetails = Map.ofEntries(
                Map.entry("name", "Minecraft - " + subscriptionId),
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
                    "default", allocation.allocationId()
                )),
                Map.entry("nest", Nest.MINECRAFT.getId()),
                Map.entry("external_id", subscriptionId)
            );
            
            PterodactylServerResponse response = pterodactylApplicationClient.createServer(serverDetails);
            PterodactylServer pterodactylServer = new PterodactylServer(
                subscriptionId, 
                response.attributes().uuid(),
                response.attributes().id(), 
                allocation.allocationId()
            );
            LOGGER.log(Level.INFO, String.format("[subscriptionId: %s] [pterodactylServerId: %s] Created pterodactyl server", subscriptionId, response.attributes().id()));
            return pterodactylServer;
        } catch (Exception e) {
            throw new PterodactylException(String.format("[subscriptionId: %s] Error creating pterodactyl server", subscriptionId), e);
        }
    }

    public void destroyServer(Long pterodactylServerId) {
        LOGGER.log(Level.INFO, String.format("[pterodactylServerId: %s] Deleting pterodactyl server", pterodactylServerId));
        try {
            persistenceContext.inTransaction(() -> {
                gameServerRepository.deletePterodactylServer(pterodactylServerId);
                pterodactylApplicationClient.deleteServer(pterodactylServerId);
            });
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

    public void startServer(String pterodactylServerUid) {   
        try {
            pterodactylUserClient.setPowerState(pterodactylServerUid, PowerState.START);
            LOGGER.log(Level.INFO, String.format("[pterodactylServerUid: %s] Started pterodactyl server", pterodactylServerUid));
        } catch (Exception e) {
            throw new PterodactylException(String.format("[pterodactylServerUid: %s] Error starting pterodactyl server", pterodactylServerUid), e);
        }
    }

    public void acceptEula(String pterodactylServerUid) {      
        try {
            pterodactylUserClient.acceptMinecraftEula(pterodactylServerUid);
            LOGGER.log(Level.INFO, String.format("[pterodactylServerUid: %s] Accepting EULA for pterodactyl server", pterodactylServerUid));
        } catch (Exception e) {
            throw new PterodactylException(String.format("[pterodactylServerUid: %s] Error accepting EULA for pterodactyl server", pterodactylServerUid), e);
        }
    }

    public ServerStatus getServerStatus(String pterodactylServerUid) {   
        try {
            return pterodactylUserClient.getServerStatus(pterodactylServerUid);
        } catch (Exception e) {
            throw new PterodactylException(String.format("[pterodactylServerUid: %s] Error checking pterodactyl server status", pterodactylServerUid), e);
        }
    }

    public void reinstallServer(Long pterodactylServerId) {
        try {
            pterodactylApplicationClient.reinstallServer(pterodactylServerId);
        } catch (Exception e) {
            throw new PterodactylException(String.format("[pterodactylServerId: %s] Error reinstalling pterodactyl server", pterodactylServerId), e);
        }       
    }

    public void startNewPterodactylServer(PterodactylServer pterodactylServer) {
        try {
            final int MAX_REINSTALLS = 3;
            final int MAX_START_ATTEMPTS = 5;
            final int MAX_WAIT_CYCLES = 30;
            final int POLL_INTERVAL_MS = 5000;
            
            Long serverId = pterodactylServer.pterodactylServerId();
            String serverUid = pterodactylServer.pterodactylServerUid();
            
            for (int reinstall = 0; reinstall <= MAX_REINSTALLS; reinstall++) {
                if (reinstall > 0) {
                    LOGGER.info(String.format("[serverId: %s] reinstalling server (attempt %d/%d)", 
                        serverId, reinstall, MAX_REINSTALLS));
                    reinstallServer(serverId);
                    Thread.sleep(POLL_INTERVAL_MS);
                }
                
                for (int attempt = 1; attempt <= MAX_START_ATTEMPTS; attempt++) {
                    try {
                        ServerStatus status = getServerStatus(serverUid);
                        
                        if (status == ServerStatus.RUNNING) {
                            return;
                        }
                        
                        if (List.of(ServerStatus.STOPPING, ServerStatus.STOPPED, ServerStatus.OFFLINE).contains(status)) {
                            LOGGER.info(String.format("[serverId: %s] starting server (attempt %d/%d)", 
                                serverId, attempt, MAX_START_ATTEMPTS));
                            startServer(serverUid);
                            acceptEula(serverUid);
                        }
                        
                        for (int wait = 0; wait < MAX_WAIT_CYCLES; wait++) {
                            Thread.sleep(POLL_INTERVAL_MS);
                            status = getServerStatus(serverUid);
                            
                            if (status == ServerStatus.RUNNING) {
                                return;
                            }
                            
                            if (status != ServerStatus.STARTING) {
                                break;
                            }
                        }
                        

                    } catch (Exception e) {
                        LOGGER.warning(String.format("[serverId: %s] start attempt %d failed: %s", 
                            serverId, attempt, e.getMessage()));
                        if (attempt == MAX_START_ATTEMPTS) {
                            break;
                        }
                        Thread.sleep(POLL_INTERVAL_MS);
                    }
                }
            }
            
            throw new RuntimeException(String.format(
                "[serverId: %s] server failed to start after %d reinstalls", 
                serverId, MAX_REINSTALLS
            ));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PterodactylException("server startup interrupted", e);
        }

    }
}

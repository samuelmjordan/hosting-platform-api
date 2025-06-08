package com.mc_host.api.service.resources;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.client.PterodactylApplicationClient;
import com.mc_host.api.client.PterodactylUserClient;
import com.mc_host.api.client.PterodactylUserClient.ServerStatus;
import com.mc_host.api.model.resource.DnsARecord;
import com.mc_host.api.model.resource.hetzner.HetznerRegion;
import com.mc_host.api.model.resource.pterodactyl.PowerState;
import com.mc_host.api.model.resource.pterodactyl.PterodactylAllocation;
import com.mc_host.api.model.resource.pterodactyl.PterodactylNode;
import com.mc_host.api.model.resource.pterodactyl.PterodactylServer;
import com.mc_host.api.model.resource.pterodactyl.games.Egg;
import com.mc_host.api.model.resource.pterodactyl.games.Nest;
import com.mc_host.api.model.resource.pterodactyl.request.PterodactylCreateNodeRequest;

@Service
public class PterodactylService {
    private static final Logger LOGGER = Logger.getLogger(PterodactylService.class.getName());

    private final PterodactylApplicationClient appClient;
    private final PterodactylUserClient userClient;
    private final WingsService wingsService;
    
    public PterodactylService(
        PterodactylApplicationClient appClient,
        PterodactylUserClient userClient,
        WingsService wingsService
    ) {
        this.appClient = appClient;
        this.userClient = userClient;
        this.wingsService = wingsService;
    }

    public PterodactylNode createNode(DnsARecord dnsARecord) {   
        var nodeRequest = PterodactylCreateNodeRequest.builder()
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
            
        var nodeResponse = appClient.createNode(nodeRequest);
        var node = new PterodactylNode(
            dnsARecord.subscriptionId(),
            nodeResponse.attributes().id()
        );
        
        LOGGER.info("[aRecordId: %s] [nodeId: %s] created pterodactyl node".formatted(
            dnsARecord.aRecordId(), nodeResponse.attributes().id()));
        return node;
    }

    public void destroyNode(Long nodeId) {
        appClient.deleteNode(nodeId);
        LOGGER.info("[nodeId: %s] deleted pterodactyl node".formatted(nodeId));
    }

    public void configureNode(Long nodeId, DnsARecord dnsARecord) {
        var wingsConfig = getNodeConfig(nodeId);
        wingsService.setupWings(dnsARecord, wingsConfig);
        LOGGER.info("[aRecordId: %s] set up wings".formatted(dnsARecord.aRecordId()));
    }

    private String getNodeConfig(Long nodeId) {
        var config = appClient.getNodeConfiguration(nodeId);
        LOGGER.info("[nodeId: %s] fetched wings config".formatted(nodeId));
        return config;
    }

    public void createAllocation(Long nodeId, String ipv4, Integer port) {
        appClient.createMultipleAllocations(nodeId, ipv4, List.of(port), "Minecraft");
        LOGGER.info("[nodeId: %s] created allocation".formatted(nodeId));
    }

    public PterodactylAllocation getAllocation(String subscriptionId, Long nodeId) {
        var unassigned = appClient.getUnassignedAllocations(nodeId);
        LOGGER.info("[nodeId: %s] fetched allocation".formatted(nodeId));
        
        var attrs = unassigned.get(0).attributes();
        return new PterodactylAllocation(
            subscriptionId,
            attrs.id(),
            attrs.ip(),
            attrs.port(),
            attrs.alias()
        );
    }

    public PterodactylServer createServer(String subscriptionId, PterodactylAllocation allocation) {
        var serverDetails = Map.ofEntries(
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
            Map.entry("external_id", subscriptionId + "::" + UUID.randomUUID())
        );
        
        var response = appClient.createServer(serverDetails);
        var server = new PterodactylServer(
            subscriptionId, 
            response.attributes().uuid(),
            response.attributes().id(), 
            allocation.allocationId()
        );
        
        LOGGER.info("[subscriptionId: %s] [serverId: %s] created pterodactyl server".formatted(
            subscriptionId, response.attributes().id()));
        return server;
    }

    public void destroyServer(Long serverId) {
        appClient.deleteServer(serverId);
        LOGGER.info("[serverId: %s] deleted pterodactyl server".formatted(serverId));
    }

    public void startServer(String serverUid) {   
        userClient.setPowerState(serverUid, PowerState.START);
        LOGGER.info("[serverUid: %s] started pterodactyl server".formatted(serverUid));
    }

    public void acceptEula(String serverUid) {      
        userClient.acceptMinecraftEula(serverUid);
        LOGGER.info("[serverUid: %s] accepted EULA for pterodactyl server".formatted(serverUid));
    }

    public ServerStatus getServerStatus(String serverUid) {   
        return userClient.getServerStatus(serverUid);
    }

    public void reinstallServer(Long serverId) {
        appClient.reinstallServer(serverId);
    }

    public void startNewPterodactylServer(PterodactylServer server) {
        var serverId = server.pterodactylServerId();
        var serverUid = server.pterodactylServerUid();

        waitForServerAccessible(serverUid, serverId, Duration.ofMinutes(3));
        startAndWaitForServer(serverUid, serverId, Duration.ofMinutes(10));
    }

    private void waitForServerAccessible(String serverUid, Long serverId, Duration timeout) {
        var interval = Duration.ofSeconds(5);
        var timePassed = Duration.ZERO;
        
        while (timePassed.compareTo(timeout) < 0) {
            try {
                Thread.sleep(interval.toMillis());
                timePassed = timePassed.plus(interval);
                
                getServerStatus(serverUid);
                return; // success
                
            } catch (Exception e) {
                if (timePassed.compareTo(timeout) >= 0) {
                    throw new RuntimeException("couldn't obtain status of pterodactyl server %s after %s"
                        .formatted(serverId, timeout));
                }
            }
        }
    }

    private void startAndWaitForServer(String serverUid, Long serverId, Duration timeout) {
        var interval = Duration.ofSeconds(30);
        var timePassed = Duration.ZERO;
        
        while (timePassed.compareTo(timeout) < 0) {
            try {
                Thread.sleep(interval.toMillis());
                timePassed = timePassed.plus(interval);
                
                var status = getServerStatus(serverUid);
                
                if (status == ServerStatus.RUNNING) {
                    return; // we're done
                }
                
                if (List.of(ServerStatus.STOPPING, ServerStatus.STOPPED, ServerStatus.OFFLINE).contains(status)) {
                    LOGGER.info("[serverId: %s] starting server (%ss/%ss)".formatted(serverId, timePassed.getSeconds(), timeout.getSeconds()));
                    startServer(serverUid);
                    Thread.sleep(Duration.ofSeconds(15).toMillis()); // wait a bit
                    acceptEula(serverUid);
                }

            } catch (Exception e) {
                if (timePassed.compareTo(timeout) >= 0) {
                    throw new RuntimeException("couldn't start pterodactyl server %s after %s"
                        .formatted(serverId, timeout));
                }
                // continue retrying if we haven't timed out
            }
        }
        
        throw new RuntimeException("pterodactyl server %s didn't start within %s"
            .formatted(serverId, timeout));
    }
}
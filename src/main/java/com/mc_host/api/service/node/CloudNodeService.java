package com.mc_host.api.service.node;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.client.CloudflareClient;
import com.mc_host.api.client.CloudflareClient.DNSRecord;
import com.mc_host.api.client.HetznerClient;
import com.mc_host.api.client.PterodactylClient;
import com.mc_host.api.configuration.ApplicationConfiguration;
import com.mc_host.api.exceptions.provisioning.CloudflareProvisioningException;
import com.mc_host.api.exceptions.provisioning.HetznerProvisioningException;
import com.mc_host.api.exceptions.provisioning.PterodactylProvisioningException;
import com.mc_host.api.exceptions.provisioning.SshProvisioningException;
import com.mc_host.api.model.hetzner.HetznerRegion;
import com.mc_host.api.model.hetzner.HetznerServerResponse.Server;
import com.mc_host.api.model.hetzner.HetznerServerType;
import com.mc_host.api.model.node.Node;
import com.mc_host.api.model.pterodactyl.request.PterodactylCreateNodeRequest;
import com.mc_host.api.model.pterodactyl.response.PterodactylNodeResponse;
import com.mc_host.api.persistence.NodeRepository;
import com.mc_host.api.service.product.WingsConfigService;

@Service
public class CloudNodeService {
    private static final Logger LOGGER = Logger.getLogger(CloudNodeService.class.getName());
    private static final Integer DEFAULT_MINECRAFT_PORT = 25565;

    private final ApplicationConfiguration applicationConfiguration;
    private final NodeRepository nodeRepository;
    private final WingsConfigService wingsConfigClient;
    private final HetznerClient hetznerClient;
    private final PterodactylClient pterodactylClient;
    private final CloudflareClient cloudflareClient;

    CloudNodeService(
        ApplicationConfiguration applicationConfiguration,
        NodeRepository nodeRepository,
        WingsConfigService wingsConfigClient,
        HetznerClient hetznerClient,
        PterodactylClient pterodactylClient,
        CloudflareClient cloudflareClient
    ) {
        this.applicationConfiguration = applicationConfiguration;
        this.nodeRepository = nodeRepository;
        this.hetznerClient = hetznerClient;
        this.pterodactylClient = pterodactylClient;
        this.wingsConfigClient = wingsConfigClient;
        this.cloudflareClient = cloudflareClient;
    }

    public Node provisionCloudNode(HetznerRegion hetznerRegion, HetznerServerType  hetznerServerType) {
        Node node = Node.newCloudNode();
        try {    
            nodeRepository.insertNewNode(node);
            provisionHetznerNode(node, hetznerRegion, hetznerServerType);
            createARecord(node);
            configurePterodactylNode(node);
            createAllocations(node);
            installWings(node);

            LOGGER.log(Level.INFO, String.format("Cloud node %s READY", node.getNodeId()));
            return node;
        } catch(HetznerProvisioningException e) {
            destroyHetznerNode(node);
            throw e;
        } catch(CloudflareProvisioningException e) {
            destroyHetznerNode(node);
            throw e;
        } catch(PterodactylProvisioningException e) {
            destroyDNSRecord(node);
            destroyHetznerNode(node);
            throw e;
        } catch(SshProvisioningException e) {
            destroyPterodactylNode(node);
            destroyDNSRecord(node);
            destroyHetznerNode(node);
            throw e;
        }   
    }

    private void provisionHetznerNode(Node node, HetznerRegion hetznerRegion, HetznerServerType  hetznerServerType) throws  HetznerProvisioningException {
        // Create hetzner node
        LOGGER.log(Level.INFO, String.format("[node: %s] Provisioning with hetzner", node.getNodeId()));
        Server hetznerServer = null;
        try {
            hetznerServer = hetznerClient.createServer(
                "nodeId." + node.getNodeId(),
                hetznerServerType.toString(),
                hetznerRegion.toString(),
                "ubuntu-24.04"
            ).server;
        } catch (Exception e) {
            Long hetznerNodeId = hetznerServer == null ? null : hetznerServer.id;
            throw new HetznerProvisioningException(
                "Create hetzner node request failed",
                e,
                node.getNodeId(),
                hetznerNodeId);
        }

        try {
            // If it  fails to come up, then teardown
            LOGGER.log(Level.INFO, String.format("[node: %s] Waiting for hetzner node to start", node.getNodeId()));
            if (!hetznerClient.waitForServerStatus(hetznerServer.id, "running")) {
                hetznerClient.deleteServer(hetznerServer.id);
                throw new RuntimeException(String.format("Hetzner node %s failed to reach running state within timeout", node.getHetznerNodeId()));
            }

            // Update DB state
            node.setHetznerNodeId(hetznerServer.id);
            node.setIpv4(hetznerServer.public_net.ipv4.ip);
            node.setHetznerRegion(hetznerRegion);
            nodeRepository.updateNode(node);        
        } catch (Exception e) {
            throw new HetznerProvisioningException(
                "Time-out waiting for hetzner node to start",
                e,
                node.getNodeId(),
                node.getHetznerNodeId());
        }

    }

    private void createARecord(Node node) throws  CloudflareProvisioningException {
        // Create DNS records with cloudflare
        LOGGER.log(Level.INFO, String.format("[node: %s] [hetznerNode: %s] Creating DNS records with cloudflare", node.getNodeId(), node.getHetznerNodeId()));
        try {
            String zoneName = applicationConfiguration.getDomain();
            String recordName = node.getNodeId().replace("-", "");
            DNSRecord aRecord = cloudflareClient.createARecord(zoneName, recordName, node.getIpv4(), false);
            node.setARecordId(aRecord.id());
            node.setZoneName(zoneName);
            node.setRecordName(recordName);
            nodeRepository.updateNode(node);
        } catch (Exception e) {
            throw new CloudflareProvisioningException(
                "Failed to create cloudflare dns records",
                e,
                node.getNodeId(),
                node.getHetznerNodeId());
        }
    }

    private void configurePterodactylNode(Node node) throws  HetznerProvisioningException {
        // Configure pterodactyl node
        LOGGER.log(Level.INFO, String.format("[node: %s] [hetznerNode: %s] Registering node with pterodactyl", node.getNodeId(), node.getHetznerNodeId()));
        PterodactylCreateNodeRequest pterodactylNode = null;
        PterodactylNodeResponse pterodactylResponse = null;
        try {
            pterodactylNode = PterodactylCreateNodeRequest.builder()
                .name(node.getNodeId())
                .description(node.getNodeId())
                .locationId(HetznerRegion.NBG1.getPterodactylLocationId())
                .public_(true)
                .fqdn(String.join(".", node.getNodeId().replace("-", ""), applicationConfiguration.getDomain()))
                .scheme(applicationConfiguration.getScheme())
                .memory(1024)
                .memoryOverallocate(0)
                .disk(50000)
                .diskOverallocate(0)
                .uploadSize(100)
                .daemonSftp(2022)
                .daemonListen(8080)
                .build();
            pterodactylResponse = pterodactylClient.createNode(pterodactylNode);
            node.setPterodactylNodeId(pterodactylResponse.attributes().id());
            nodeRepository.updateNode(node);
        } catch (Exception e) {
            Long pterodactylNodeId = pterodactylResponse == null ? null : pterodactylResponse.attributes().id();
            throw new PterodactylProvisioningException(
                "Failed to create cloudflare dns records",
                e,
                node.getNodeId(),
                node.getHetznerNodeId(),
                pterodactylNodeId);
        }
    }

    private void createAllocations(Node node) throws PterodactylProvisioningException {
        LOGGER.log(Level.INFO, String.format("[node: %s] [hetznerNode: %s] Creating port allocations", 
            node.getNodeId(), node.getHetznerNodeId()));
        try {
            pterodactylClient.createMultipleAllocations(
                node.getPterodactylNodeId(),
                node.getIpv4(),
                List.of(DEFAULT_MINECRAFT_PORT),
                "Minecraft"
            );
            
            LOGGER.log(Level.INFO, String.format("[node: %s] Successfully created allocation %d", 
                node.getNodeId(), DEFAULT_MINECRAFT_PORT));
        } catch (Exception e) {
            throw new PterodactylProvisioningException(
                "Failed to create allocations on pterodactyl node",
                e,
                node.getNodeId(),
                node.getHetznerNodeId(),
                node.getPterodactylNodeId());
        }
    }

    private void installWings(Node node) throws  SshProvisioningException {
        // Install wings
        LOGGER.log(Level.INFO, String.format("[node: %s] [hetznerNode: %s] Installing wings via ssh", node.getNodeId(), node.getHetznerNodeId()));
        try {
            String wingsConfig = pterodactylClient.getNodeConfiguration(node.getPterodactylNodeId());
            wingsConfigClient.setupWings(node, wingsConfig);
        } catch (Exception e) {
            throw new SshProvisioningException(
                "Failed to install wings",
                e,
                node.getNodeId(),
                node.getHetznerNodeId(),
                node.getPterodactylNodeId());
        }
    }

    public void destroyCloudNode(String nodeId) {
        Node node = nodeRepository.selectNode(nodeId)
            .orElseThrow(() -> new RuntimeException(String.format("No node %s could be found", nodeId)));
        try {    
            destroyPterodactylNode(node);
            destroyHetznerNode(node);
            destroyDNSRecord(node);
            nodeRepository.deleteNode(nodeId);
            LOGGER.log(Level.INFO, String.format("Cloud node %s DESTROYED", nodeId));
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
    }

    private void destroyPterodactylNode(Node node) {
        LOGGER.log(Level.INFO, String.format("[node: %s] [hetznerNode: %s] Registering node with pterodactyl", node.getNodeId(), node.getHetznerNodeId()));
        try {
            pterodactylClient.deleteNode(node.getPterodactylNodeId());
            node.setPterodactylNodeId(null);
            nodeRepository.updateNode(node);
        } catch (Exception e) {
            throw new PterodactylProvisioningException(
                "Failed to destroy pterodactyl node",
                e,
                node.getNodeId(),
                node.getHetznerNodeId(),
                node.getPterodactylNodeId());
        }
    }

    private void destroyHetznerNode(Node node) {
        LOGGER.log(Level.INFO, String.format("[node: %s] [hetznerNodeId: %s] Destroying hetzner node", node.getNodeId(), node.getHetznerNodeId()));
        try {
            hetznerClient.deleteServer(node.getHetznerNodeId());
            node.setHetznerNodeId(null);
            node.setHetznerRegion(null);
            nodeRepository.updateNode(node);
        } catch (Exception e) {
            throw new HetznerProvisioningException(
                "Destroy hetzner node request failed",
                e,
                node.getNodeId(),
                node.getHetznerNodeId());
        }
    }

    private void destroyDNSRecord(Node node) {
        LOGGER.log(Level.INFO, String.format("[node: %s] [hetznerNode: %s] [aRecord: %s]Destroying DNS records with cloudflare", node.getNodeId(), node.getHetznerNodeId(), node.getARecordId()));
        try {
            cloudflareClient.deleteDNSRecord(node.getZoneName(), node.getARecordId());
            node.setARecordId(null);
            node.setZoneName(null);
            node.setRecordName(null);
            node.setIpv4(null);
            nodeRepository.updateNode(node);
        } catch (Exception e) {
            throw new CloudflareProvisioningException(
                "Failed to destroy cloudflare dns records",
                e,
                node.getNodeId(),
                node.getHetznerNodeId());
        }
    }
}

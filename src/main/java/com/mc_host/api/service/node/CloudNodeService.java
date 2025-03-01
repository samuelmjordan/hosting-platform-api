package com.mc_host.api.service.node;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mc_host.api.client.CloudflareClient;
import com.mc_host.api.client.CloudflareClient.DNSRecordResponse;
import com.mc_host.api.client.HetznerClient;
import com.mc_host.api.client.PterodactylClient;
import com.mc_host.api.configuration.ApplicationConfiguration;
import com.mc_host.api.exceptions.provisioning.CloudflareProvisioningException;
import com.mc_host.api.exceptions.provisioning.HetznerProvisioningException;
import com.mc_host.api.exceptions.provisioning.NodeProvisioningException;
import com.mc_host.api.exceptions.provisioning.PterodactylProvisioningException;
import com.mc_host.api.exceptions.provisioning.SshProvisioningException;
import com.mc_host.api.model.hetzner.HetznerRegion;
import com.mc_host.api.model.hetzner.HetznerServerResponse.Server;
import com.mc_host.api.model.hetzner.HetznerServerType;
import com.mc_host.api.model.node.DnsARecord;
import com.mc_host.api.model.node.HetznerNode;
import com.mc_host.api.model.node.Node;
import com.mc_host.api.model.node.PterodactylNode;
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
            nodeRepository.insertNode(node);
            HetznerNode hetznerNode = provisionHetznerNode(node, hetznerRegion, hetznerServerType);
            waitForHetznerNode(hetznerNode);
            DnsARecord dnsARecord = createARecord(hetznerNode);
            PterodactylNode pterodactylNode = configurePterodactylNode(hetznerNode, dnsARecord);
            createAllocations(hetznerNode, pterodactylNode);
            installWings(pterodactylNode, hetznerNode, dnsARecord);
            LOGGER.log(Level.INFO, String.format("Cloud node %s READY", node.nodeId()));
            return node;
        } catch(NodeProvisioningException e) {
            destroyCloudNode(node.nodeId());
            throw e;
        }
    }

    private HetznerNode provisionHetznerNode(Node node, HetznerRegion hetznerRegion, HetznerServerType  hetznerServerType) throws  HetznerProvisioningException {
        // Create hetzner node
        LOGGER.log(Level.INFO, String.format("[node: %s] Provisioning with hetzner", node.nodeId()));
        HetznerNode hetznerNode = null;
        try {
            Server hetznerServer = hetznerClient.createServer(
                "nodeId." + node.nodeId(),
                hetznerServerType.toString(),
                hetznerRegion.toString(),
                "ubuntu-24.04"
            ).server;
            hetznerNode = new HetznerNode(node.nodeId(), hetznerServer.id, hetznerRegion, hetznerServer.public_net.ipv4.ip);
            nodeRepository.insertHetznerNode(hetznerNode);
        } catch (Exception e) {
            Long hetznerNodeId = hetznerNode == null ? null : hetznerNode.hetznerNodeId();
            throw new HetznerProvisioningException(
                "Create hetzner node request failed",
                e,
                node.nodeId(),
                hetznerNodeId);
        }
        return hetznerNode;
    }

    private void waitForHetznerNode(HetznerNode hetznerNode) {
        try {
            // If it  fails to come up, then teardown
            LOGGER.log(Level.INFO, String.format("[node: %s] Waiting for hetzner node to start", hetznerNode.nodeId()));
            if (!hetznerClient.waitForServerStatus(hetznerNode.hetznerNodeId(), "running")) {
                throw new RuntimeException(String.format("Hetzner node %s failed to reach running state within timeout", hetznerNode.hetznerNodeId()));
            }      
        } catch (Exception e) {
            throw new HetznerProvisioningException(
                "Time-out waiting for hetzner node to start",
                e,
                hetznerNode.nodeId(),
                hetznerNode.hetznerNodeId());
        }
    }

    private DnsARecord createARecord(HetznerNode hetznerNode) throws  CloudflareProvisioningException {
        // Create DNS records with cloudflare
        LOGGER.log(Level.INFO, String.format("[node: %s] [hetznerNode: %s] Creating DNS records with cloudflare", hetznerNode.nodeId(), hetznerNode.hetznerNodeId()));
        DnsARecord dnsARecord;
        try {
            String zoneName = applicationConfiguration.getDomain();
            String recordName = hetznerNode.nodeId().replace("-", "");
            DNSRecordResponse dnsARecordResponse = cloudflareClient.createARecord(zoneName, recordName, hetznerNode.ipv4(), false);
            dnsARecord = new DnsARecord(hetznerNode.nodeId(), dnsARecordResponse.id(), dnsARecordResponse.zoneName(), dnsARecordResponse.name(), hetznerNode.ipv4());
            nodeRepository.insertDnsARecord(dnsARecord);
        } catch (Exception e) {
            throw new CloudflareProvisioningException(
                "Failed to create cloudflare dns records",
                e,
                hetznerNode.nodeId(),
                hetznerNode.hetznerNodeId());
        }
        return dnsARecord;
    }

    private PterodactylNode configurePterodactylNode(HetznerNode hetznerNode, DnsARecord dnsARecord) throws  HetznerProvisioningException {
        // Configure pterodactyl node
        LOGGER.log(Level.INFO, String.format("[node: %s] Registering node with pterodactyl", dnsARecord.nodeId()));
        PterodactylNode pterodactylNode = null;
        try {
            PterodactylCreateNodeRequest pterodactylNodeRequest = PterodactylCreateNodeRequest.builder()
                .name(dnsARecord.nodeId())
                .description(dnsARecord.nodeId())
                .locationId(HetznerRegion.NBG1.getPterodactylLocationId())
                .public_(true)
                .fqdn(dnsARecord.recordName())
                .scheme(applicationConfiguration.getScheme())
                .memory(1024)
                .memoryOverallocate(0)
                .disk(50000)
                .diskOverallocate(0)
                .uploadSize(100)
                .daemonSftp(2022)
                .daemonListen(8080)
                .build();
            PterodactylNodeResponse pterodactylNodeResponse = pterodactylClient.createNode(pterodactylNodeRequest);
            pterodactylNode = new PterodactylNode(dnsARecord.nodeId(), pterodactylNodeResponse.attributes().id());
            nodeRepository.insertPterodactylNode(pterodactylNode);
        } catch (Exception e) {
            Long pterodactylNodeId = pterodactylNode == null ? null : pterodactylNode.pterodactylNodeId();
            throw new PterodactylProvisioningException(
                "Failed to create pterodactyl node",
                e,
                dnsARecord.nodeId(),
                null,
                pterodactylNodeId);
        }
        return pterodactylNode;
    }

    private void createAllocations(HetznerNode hetznerNode, PterodactylNode pterodactylNode) throws PterodactylProvisioningException {
        LOGGER.log(Level.INFO, String.format("[node: %s] Creating port allocations", pterodactylNode.nodeId()));
        try {
            pterodactylClient.createMultipleAllocations(
                pterodactylNode.pterodactylNodeId(),
                hetznerNode.ipv4(),
                List.of(DEFAULT_MINECRAFT_PORT),
                "Minecraft"
            );
            LOGGER.log(Level.INFO, String.format("[node: %s] Successfully created allocation %d", pterodactylNode.nodeId(), DEFAULT_MINECRAFT_PORT));
        } catch (Exception e) {
            throw new PterodactylProvisioningException(
                "Failed to create allocations on pterodactyl node",
                e,
                pterodactylNode.nodeId(),
                null,
                pterodactylNode.pterodactylNodeId());
        }
    }

    private void installWings(PterodactylNode pterodactylNode, HetznerNode hetznerNode, DnsARecord dnsARecord) throws SshProvisioningException {
        // Install wings
        LOGGER.log(Level.INFO, String.format("[node: %s] Installing wings via ssh", pterodactylNode.nodeId()));
        try {
            String wingsConfig = pterodactylClient.getNodeConfiguration(pterodactylNode.pterodactylNodeId());
            wingsConfigClient.setupWings(hetznerNode, dnsARecord, wingsConfig);
        } catch (Exception e) {
            throw new SshProvisioningException(
                "Failed to install wings",
                e,
                hetznerNode.nodeId(),
                hetznerNode.hetznerNodeId(),
                pterodactylNode.pterodactylNodeId());
        }
    }

    // TODO: Taskify
    public void destroyCloudNode(String nodeId) {
        LOGGER.log(Level.INFO, String.format("Starting teardown for node %s", nodeId));
        Optional<PterodactylNode> pterodactylNode = nodeRepository.selectPterodactylNode(nodeId);
        Optional<HetznerNode> hetznerNode = nodeRepository.selectHetznerNode(nodeId);
        Optional<DnsARecord> dnsARecord = nodeRepository.selectDnsARecord(nodeId);
        try {
            if (pterodactylNode.isPresent()) {
                destroyPterodactylNode(pterodactylNode.get());
            } // else, actually check?    
            if (hetznerNode.isPresent()) {
                destroyHetznerNode(hetznerNode.get());
            }  
            if (dnsARecord.isPresent()) {
                destroyDNSRecord(dnsARecord.get());
            }
            nodeRepository.deleteNode(nodeId);
            LOGGER.log(Level.INFO, String.format("Cloud node %s DESTROYED", nodeId));
        } catch(NodeProvisioningException e) {
            // cleanup
            throw e;
        }
    }

    @Transactional
    private void destroyPterodactylNode(PterodactylNode pterodactylNode) {
        LOGGER.log(Level.INFO, String.format("[node: %s] Destroying node with pterodactyl", pterodactylNode.nodeId()));
        try {
            nodeRepository.deletePterodactylNode(pterodactylNode.nodeId());
            pterodactylClient.deleteNode(pterodactylNode.pterodactylNodeId());
        } catch (Exception e) {
            throw new PterodactylProvisioningException(
                "Failed to destroy pterodactyl node",
                e,
                pterodactylNode.nodeId(),
                null,
                pterodactylNode.pterodactylNodeId());
        }
    }

    @Transactional
    private void destroyHetznerNode(HetznerNode hetznerNode) {
        LOGGER.log(Level.INFO, String.format("[node: %s] [hetznerNodeId: %s] Destroying hetzner node", hetznerNode.nodeId(), hetznerNode.hetznerNodeId()));
        try {
            nodeRepository.deleteHetznerNode(hetznerNode.nodeId());
            hetznerClient.deleteServer(hetznerNode.hetznerNodeId());
        } catch (Exception e) {
            throw new HetznerProvisioningException(
                "Destroy hetzner node request failed",
                e,
                hetznerNode.nodeId(),
                hetznerNode.hetznerNodeId());
        }
    }

    @Transactional
    private void destroyDNSRecord(DnsARecord dnsARecord) {
        LOGGER.log(Level.INFO, String.format("[node: %s] [aRecord: %s] Destroying DNS records with cloudflare", dnsARecord.nodeId(), dnsARecord.aRecordId()));
        try {
            nodeRepository.deleteDnsARecord(dnsARecord.nodeId());
            cloudflareClient.deleteDNSRecord(dnsARecord.zoneName(), dnsARecord.aRecordId());
        } catch (Exception e) {
            throw new CloudflareProvisioningException(
                "Failed to destroy cloudflare dns records",
                e,
                dnsARecord.nodeId(),
                null);
        }
    }
}

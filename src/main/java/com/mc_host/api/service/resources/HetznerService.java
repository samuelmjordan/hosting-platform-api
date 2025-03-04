package com.mc_host.api.service.resources;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.client.HetznerClient;
import com.mc_host.api.exceptions.resources.HetznerException;
import com.mc_host.api.model.hetzner.HetznerRegion;
import com.mc_host.api.model.hetzner.HetznerServerResponse.Server;
import com.mc_host.api.model.hetzner.HetznerServerType;
import com.mc_host.api.model.node.HetznerNode;
import com.mc_host.api.model.node.Node;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.util.PersistenceContext;

@Service
public class HetznerService {
    private static final Logger LOGGER = Logger.getLogger(HetznerService.class.getName());

    private final HetznerClient hetznerClient;
    private final NodeRepository nodeRepository;
    private final PersistenceContext persistenceContext;

    public HetznerService(
        HetznerClient hetznerClient,
        NodeRepository nodeRepository,
        PersistenceContext persistenceContext
    ) {
        this.hetznerClient = hetznerClient;
        this.nodeRepository = nodeRepository;
        this.persistenceContext = persistenceContext;
    }

    public HetznerNode createCloudNode(Node node, HetznerRegion hetznerRegion, HetznerServerType  hetznerServerType) {
        LOGGER.log(Level.INFO, String.format("[nodeId: %s] Creating hetzner cloud node", node.nodeId()));
        try {
            Server hetznerServer = hetznerClient.createServer(
                node.nodeId(),
                hetznerServerType.toString(),
                hetznerRegion.toString(),
                "ubuntu-24.04"
            ).server;
            HetznerNode hetznerNode = new HetznerNode(
                node.nodeId(), 
                hetznerServer.id, 
                hetznerRegion, 
                hetznerServer.public_net.ipv4.ip);
            nodeRepository.insertHetznerNode(hetznerNode);
            
            if (!hetznerClient.waitForServerStatus(hetznerServer.id, "running")) {
                throw new RuntimeException(String.format("[nodeId: %s] Timed-out creating hetzner cloud node", node.nodeId()));
            }
            LOGGER.log(Level.INFO, String.format("[nodeId: %s] Created hetzner cloud node", node.nodeId()));
            return hetznerNode; 
        } catch (Exception e) {
            throw new HetznerException(String.format("[nodeId: %s] Error creating hetzner cloud node", node.nodeId()), e);
        }       
    }

    public void deleteNode(Long hetznerNodeId) {
        LOGGER.log(Level.INFO, String.format("[hetznerNodeId: %s] Deleting hetzner node", hetznerNodeId));
        try {
            persistenceContext.inTransaction(() -> {
                nodeRepository.deleteHetznerNode(hetznerNodeId);
                hetznerClient.deleteServer(hetznerNodeId);
            });
            LOGGER.log(Level.INFO, String.format("[hetznerNodeId: %s] Deleted hetzner node", hetznerNodeId));
        } catch (Exception e) {
            throw new HetznerException(String.format("[hetznerNodeId: %s] Error deleting hetzner node", hetznerNodeId), e);
        }  
    }

    public void deleteNodeWithGameServerId(String nodeId) {
        HetznerNode hetznerNode = nodeRepository.selectHetznerNode(nodeId)
            .orElseThrow(() -> new IllegalStateException(String.format("[gameServerId: %s] No hetzner node associated with node", nodeId)));
        deleteNode(hetznerNode.hetznerNodeId());
    }
    
}

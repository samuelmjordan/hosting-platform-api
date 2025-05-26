package com.mc_host.api.service.resources;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.client.HetznerCloudClient;
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

    private final HetznerCloudClient hetznerClient;
    private final NodeRepository nodeRepository;
    private final PersistenceContext persistenceContext;

    public HetznerService(
        HetznerCloudClient hetznerClient,
        NodeRepository nodeRepository,
        PersistenceContext persistenceContext
    ) {
        this.hetznerClient = hetznerClient;
        this.nodeRepository = nodeRepository;
        this.persistenceContext = persistenceContext;
    }

    public HetznerNode createCloudNode(String subscriptionId, HetznerRegion hetznerRegion, HetznerServerType  hetznerServerType) {
        try {
            String uuid = UUID.randomUUID().toString();
            Server hetznerServer = hetznerClient.createServer(
                uuid,
                hetznerServerType.toString(),
                hetznerRegion.toString(),
                "ubuntu-24.04"
            ).server;
            HetznerNode hetznerNode = new HetznerNode(
                subscriptionId,
                hetznerServer.id, 
                hetznerRegion, 
                hetznerServer.public_net.ipv4.ip
            );
            
            if (!hetznerClient.waitForServerStatus(hetznerServer.id, "running")) {
                throw new RuntimeException(String.format("[subscriptionId: %s] [nodeId: %s] Timed-out creating hetzner cloud node", subscriptionId, hetznerServer.id));
            }
            LOGGER.log(Level.INFO, String.format("[subscriptionId: %s] [nodeId: %s] Created hetzner cloud node", subscriptionId, hetznerServer.id));
            return hetznerNode; 
        } catch (Exception e) {
            throw new HetznerException(String.format("[subscriptionId: %s] Error creating hetzner cloud node", subscriptionId), e);
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
    
}

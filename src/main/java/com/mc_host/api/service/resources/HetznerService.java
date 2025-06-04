package com.mc_host.api.service.resources;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.client.HetznerCloudClient;
import com.mc_host.api.exceptions.resources.HetznerException;
import com.mc_host.api.model.resource.hetzner.HetznerNode;
import com.mc_host.api.model.resource.hetzner.HetznerRegion;
import com.mc_host.api.model.resource.hetzner.HetznerServerResponse;
import com.mc_host.api.model.resource.hetzner.HetznerSpec;
import com.mc_host.api.model.resource.hetzner.HetznerServerResponse.Server;

@Service
public class HetznerService {
    private static final Logger LOGGER = Logger.getLogger(HetznerService.class.getName());

    private final HetznerCloudClient hetznerClient;
    public HetznerService(
        HetznerCloudClient hetznerClient
    ) {
        this.hetznerClient = hetznerClient;
    }

    public HetznerNode createCloudNode(String subscriptionId, HetznerRegion hetznerRegion, HetznerSpec hetznerServerType) {
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
                hetznerServerType,
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

    public void deleteCloudNode(Long hetznerNodeId) {
        LOGGER.log(Level.INFO, String.format("[hetznerNodeId: %s] Deleting hetzner node", hetznerNodeId));
        try {
            hetznerClient.deleteServer(hetznerNodeId);
            LOGGER.log(Level.INFO, String.format("[hetznerNodeId: %s] Deleted hetzner node", hetznerNodeId));
        } catch (Exception e) {
            throw new HetznerException(String.format("[hetznerNodeId: %s] Error deleting hetzner node", hetznerNodeId), e);
        }  
    }

    public HetznerRegion getServerRegion(Long nodeId) {
        try {
            LOGGER.log(Level.INFO, String.format("[hetznerServerId: %s] Checking server region", nodeId));
            HetznerServerResponse response = hetznerClient.getServer(nodeId);
            HetznerRegion region = HetznerRegion.valueOf(response.server.datacenter.location.name.toUpperCase());
            return region;
        } catch (Exception e) {
            throw new HetznerException(String.format("[hetznerServerId: %s] Error checking server region", nodeId), e);
        }
    }
    
}

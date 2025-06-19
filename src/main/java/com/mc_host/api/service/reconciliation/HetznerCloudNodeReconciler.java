package com.mc_host.api.service.reconciliation;

import com.mc_host.api.client.HetznerCloudClient;
import com.mc_host.api.model.resource.ResourceType;
import com.mc_host.api.repository.NodeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


@Service
public class HetznerCloudNodeReconciler implements ResourceReconciler {
    private static final Logger LOGGER = Logger.getLogger(HetznerCloudNodeReconciler.class.getName());

    private final HetznerCloudClient hetznerClient;
    private final NodeRepository nodeRepository;

    HetznerCloudNodeReconciler(
        HetznerCloudClient hetznerClient,
        NodeRepository nodeRepository
    ) {
        this.hetznerClient = hetznerClient;
        this.nodeRepository = nodeRepository;
    }

    @Override
    public ResourceType getType() {
        return ResourceType.HETZNER_NODE;
    }

    @Override
    public void reconcile() {
        LOGGER.log(Level.FINE, "Reconciling hetzner nodes with db");
        try {
            List<Long> actualHetznerNodes = fetchActualResources();
            List<Long> expectedHetznerNodes = fetchExpectedResources();
            List<Long> nodesToDestroy = actualHetznerNodes.stream()
                .filter(hetznerNodeId -> expectedHetznerNodes.stream().noneMatch(hetznerNodeId::equals))
                .toList();

            if (nodesToDestroy.isEmpty()) return;

            LOGGER.log(Level.INFO, String.format("Found %s hetzner nodes to destroy", nodesToDestroy.size()));

            nodesToDestroy.forEach(nodeId -> {
				try {
					hetznerClient.deleteServer(nodeId);
				} catch (Exception e) {
                    LOGGER.warning("Exception caught destroying cloud node %s: %s".formatted(nodeId, e));
                }
			});
        } catch (Exception e) {
            throw new RuntimeException("Failed hetzner node reconciliation", e);
        }
    }
    
    private List<Long> fetchActualResources() throws Exception {
        return hetznerClient.getAllServers().stream()
            .map(response -> response.id).toList();
    }

    private List<Long> fetchExpectedResources() {
        return nodeRepository.selectAllHetznerNodeIds();
    }
    
}

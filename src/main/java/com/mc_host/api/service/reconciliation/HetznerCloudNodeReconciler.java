package com.mc_host.api.service.reconciliation;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.client.HetznerCloudClient;
import com.mc_host.api.model.resource.ResourceType;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.util.Task;


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
        LOGGER.log(Level.INFO, String.format("Reconciling hetzner nodes with db"));
        try {
            List<Long> actualHetznerNodes = fetchActualResources();
            List<Long> expectedHetznerNodes = fetchExpectedResources();
            List<Long> subscriptionsToDestroy = actualHetznerNodes.stream()
                .filter(hetznerNodeId -> expectedHetznerNodes.stream().noneMatch(hetznerNodeId::equals))
                .toList();
            LOGGER.log(Level.INFO, String.format("Found %s hetzner nodes to destroy", subscriptionsToDestroy.size()));

            if (subscriptionsToDestroy.size() == 0) return;

            List<CompletableFuture<Void>> deleteTasks = subscriptionsToDestroy.stream()
                .map(hetznerNodeId -> Task.alwaysAttempt(
                    "Delete hetznerNode " + hetznerNodeId,
                    () -> {
                        hetznerClient.deleteServer(hetznerNodeId);
                    }
                )).toList();

            Task.awaitCompletion(deleteTasks);
            LOGGER.log(Level.INFO, "Executed hetzner node reconciliation");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed hetzner node reconciliation", e);
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

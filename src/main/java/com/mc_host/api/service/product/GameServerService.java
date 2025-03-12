package com.mc_host.api.service.product;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.client.PterodactylApplicationClient.AllocationAttributes;
import com.mc_host.api.client.PterodactylUserClient.ServerStatus;
import com.mc_host.api.exceptions.resources.DeprovisioningException;
import com.mc_host.api.exceptions.resources.PterodactylException;
import com.mc_host.api.model.MarketingRegion;
import com.mc_host.api.model.MetadataKey;
import com.mc_host.api.model.entity.ContentSubscription;
import com.mc_host.api.model.game_server.GameServer;
import com.mc_host.api.model.game_server.PterodactylServer;
import com.mc_host.api.model.hetzner.HetznerRegion;
import com.mc_host.api.model.hetzner.HetznerServerType;
import com.mc_host.api.model.node.DnsARecord;
import com.mc_host.api.model.node.HetznerNode;
import com.mc_host.api.model.node.Node;
import com.mc_host.api.model.node.PterodactylNode;
import com.mc_host.api.model.specification.SpecificationType;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.repository.PlanRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.service.resources.DnsService;
import com.mc_host.api.service.resources.HetznerService;
import com.mc_host.api.service.resources.PterodactylService;

@Service
public class GameServerService implements SubscriptionService {
    private static final Logger LOGGER = Logger.getLogger(GameServerService.class.getName());

    private final NodeRepository nodeRepository;
    private final GameServerRepository gameServerRepository;
    private final SubscriptionRepository  subscriptionRepository;
    private final PlanRepository planRepository;
    private final HetznerService hetznerService;
    private final PterodactylService  pterodactylService;
    private final DnsService dnsService;

    GameServerService(
        NodeRepository nodeRepository,
        GameServerRepository gameServerRepository,
        SubscriptionRepository  subscriptionRepository,
        PlanRepository planRepository,
        HetznerService hetznerService,
        PterodactylService  pterodactylService,
        DnsService dnsService
    ) {
        this.nodeRepository = nodeRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.gameServerRepository = gameServerRepository;
        this.planRepository = planRepository;
        this.hetznerService = hetznerService;
        this.pterodactylService = pterodactylService;
        this.dnsService = dnsService;
    }

    @Override
    public boolean isType(SpecificationType type) {
        return type.equals(SpecificationType.GAME_SERVER);
    }

    @Override
    public void update(ContentSubscription newSubscription, ContentSubscription oldSubsccription) {
        if(!newSubscription.subscriptionId().equals(oldSubsccription.subscriptionId())) {
            throw new IllegalStateException(String.format("Mismatched subscriptions for update: %s and %s", newSubscription.subscriptionId(), oldSubsccription.subscriptionId()));
        }

        LOGGER.log(Level.INFO, String.format("Update resources for subscription %s", newSubscription.subscriptionId()));
        // update logic
    }

    @Override
    public void create(ContentSubscription subscription) {
        LOGGER.log(Level.INFO, String.format("[subscriptionId: %s] Provisioning resources for new subscription", subscription.subscriptionId()));

        HetznerRegion hetznerRegion;
        try {
            hetznerRegion = MarketingRegion.valueOf(subscription.metadata().get(MetadataKey.REGION.name())).getHetznerRegion();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("[subscriptionId: %s] region metdata is invalid / unsupported", subscription.subscriptionId()));
            hetznerRegion  = HetznerRegion.NBG1;
        }

        Node node = createCloudNode();
        try {
            HetznerNode hetznerNode = hetznerService.createCloudNode(node, hetznerRegion, HetznerServerType.CAX11);
            DnsARecord dnsARecord = dnsService.createARecord(hetznerNode);
            PterodactylNode pterodactylNode = pterodactylService.createNode(dnsARecord);

            GameServer gameServer = createGameServer(subscription, node.nodeId());
            pterodactylService.createAllocation(pterodactylNode.pterodactylNodeId(), dnsARecord.content(), 25565);
            AllocationAttributes allocationAttributes = pterodactylService.getAllocation(pterodactylNode.pterodactylNodeId());
            pterodactylService.configureNode(pterodactylNode.pterodactylNodeId(), dnsARecord);
            PterodactylServer pterodactylServer = pterodactylService.createServer(gameServer, allocationAttributes);
            dnsService.createCNameRecord(gameServer, gameServer.serverId().replace("-", ""));
            startNewPterodactylServer(pterodactylServer);

            LOGGER.log(Level.INFO, String.format("[subscriptionId: %s] Provisioned resources for new subscription", subscription.subscriptionId()));        
        } catch (Exception e1) {
            try {
                LOGGER.log(Level.SEVERE, String.format("[subscriptionId: %s] Error provisioning resources for new subscription. Initiating cleanup.", subscription.subscriptionId()));
                cleanup(subscription, node.nodeId());       
            } catch (Exception e2) {
                throw new RuntimeException(String.format("[subscriptionId: %s] Error cleaning up after subscription provisioning failure", subscription.subscriptionId()), e2);
            }
            throw new RuntimeException(String.format("[subscriptionId: %s] Error provisioning resources for new subscription", subscription.subscriptionId()), e1);     
        }
    }

    @Override
    public void delete(ContentSubscription subscription) {
        GameServer gameServer = gameServerRepository.selectGameServerFromSubscription(subscription.subscriptionId())
            .orElseThrow(() -> new IllegalStateException(String.format("[subscriptionId: %s] No game server associated with subscription", subscription.subscriptionId())));

        String gameServerId = gameServer.serverId();
        String nodeId = gameServer.nodeId();
        deleteAll(subscription, gameServerId, nodeId);
    }

    public void cleanup(ContentSubscription subscription, String nodeId) {
        String gameServerId = gameServerRepository.selectGameServerFromSubscription(subscription.subscriptionId())
            .map(GameServer::serverId)
            .orElse(null);

        deleteAll(subscription, gameServerId, nodeId);        
    }

    public void deleteAll(ContentSubscription subscription, String gameServerId, String nodeId) {
        LOGGER.log(Level.INFO, String.format("[subscriptionId: %s] Deleting resources for existing subscription", subscription.subscriptionId()));

        List<Exception> cumulativeExceptions = new ArrayList<>();

        cumulativeExceptions.addAll(executeTasks(List.of(
            () -> pterodactylService.destroyServerWithGameServerId(gameServerId),
            () -> dnsService.deleteCNameRecordWithGameServerId(gameServerId),
            () -> dnsService.deleteARecordWithGameServerId(nodeId),
            () -> hetznerService.deleteNodeWithGameServerId(nodeId)
        )));

        cumulativeExceptions.addAll(executeTasks(List.of(
            () -> pterodactylService.destroyNodeWithGameServerId(nodeId),
            () -> gameServerRepository.deleteGameServer(gameServerId)
        )));

        executeCriticalTasks(List.of(
            () -> nodeRepository.deleteNode(nodeId),
            () -> subscriptionRepository.deleteCustomerSubscription(subscription.subscriptionId(), subscription.customerId())
        ), cumulativeExceptions);

        LOGGER.log(Level.INFO, String.format("[subscriptionId: %s] Deleted resources for existing subscription", subscription.subscriptionId()));
    }

    private Node createCloudNode() {
        Node node = Node.newCloudNode();
        nodeRepository.insertNode(node);
        return node;
    }

    private GameServer createGameServer(ContentSubscription subscription, String nodeId) {
        String planId = planRepository.selectPlanIdFromPriceId(subscription.priceId())
            .orElseThrow(() -> new IllegalStateException(String.format("[subscriptionId: %s] [priceId: %s] No spec associated with price", subscription.subscriptionId(), subscription.priceId())));
        GameServer gameServer = new GameServer(
            UUID.randomUUID().toString(),
            subscription.subscriptionId(),
            planId,
            nodeId
        );
        gameServerRepository.insertGameServer(gameServer);
        return gameServer;
    }

    private void startNewPterodactylServer(PterodactylServer pterodactylServer) throws InterruptedException {
        startNewPterodactylServer(pterodactylServer, 0);
    }
    
    private void startNewPterodactylServer(PterodactylServer pterodactylServer, int reinstalls) throws InterruptedException {
        final int MAX_REINSTALLS = 3;
        final int MAX_RETRIES = 5;
        final double BACKOFF_FACTOR = 1.5;
        final int INITIAL_DELAY_MS = 2000;
        final List<ServerStatus> STOPPABLE_STATUSES = List.of(ServerStatus.STOPPING, ServerStatus.STOPPED, ServerStatus.OFFLINE);

        LOGGER.log(Level.INFO, String.format("[pterodactylServerId: %s] Attempting to start new pterodactyl server, attempt %s", pterodactylServer.pterodactylServerId(), reinstalls + 1));
        
        if (reinstalls >= MAX_REINSTALLS) {
            throw new RuntimeException(String.format(
                "[pterodactylServerUid: %s] Server failed to start after %s reinstalls", 
                pterodactylServer.pterodactylServerUid(), 
                reinstalls
            ));
        }
    
        int retries = 0;
        int delay = INITIAL_DELAY_MS;
        String serverUid = pterodactylServer.pterodactylServerUid();
        try {
            while (retries <= MAX_RETRIES) {
                Thread.sleep(delay);
                ServerStatus serverStatus = pterodactylService.getServerStatus(serverUid);

                if (ServerStatus.RUNNING.equals(serverStatus)) {
                    return;
                } 
                else if (ServerStatus.STARTING.equals(serverStatus)) {
                    continue;
                } 
                else if (STOPPABLE_STATUSES.contains(serverStatus)) {
                    try {
                        pterodactylService.startServer(serverUid);
                        Thread.sleep(delay);
                        pterodactylService.acceptEula(serverUid);
                    } catch (Exception e) {
                        delay = (int)(delay * BACKOFF_FACTOR);
                        retries++;
                        if (retries > MAX_RETRIES) {
                            throw new PterodactylException(
                                String.format("[pterodactylServerUid: %s] Server failed to start", serverUid), 
                                e
                            );
                        }
                        continue;
                    }
                } 
                else {
                    throw new IllegalStateException(String.format("Invalid server status %s", serverStatus));
                }
                
                delay = (int)(delay * BACKOFF_FACTOR);
                retries++;
            }
            
            throw new RuntimeException(String.format(
                "[pterodactylServerUid: %s] Server failed to start after %d retries", 
                serverUid, 
                MAX_RETRIES
            ));
        } catch (Exception e) {
            pterodactylService.reinstallServer(pterodactylServer.pterodactylServerId());
            Thread.sleep(delay);
            startNewPterodactylServer(pterodactylServer, reinstalls + 1);
        }
    }

    private List<Exception> executeTasks(List<Runnable> tasks) {
        return executeTasks(tasks, List.of(), false);
    }

    private List<Exception> executeCriticalTasks(List<Runnable> tasks, List<Exception> prerequisiteExceptions) {
        return executeTasks(tasks, prerequisiteExceptions, true);
    }

    private List<Exception> executeTasks(List<Runnable> tasks, List<Exception> prerequisiteExceptions, Boolean critical) {
        List<CompletableFuture<Void>> futures = tasks.stream()
            .map(task -> CompletableFuture.runAsync(task))
            .toList();
        
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        List<Exception> newExceptions = new ArrayList<>();
        try {
            allTasks.join();
        } catch (CompletionException e) {
            newExceptions.add(e);
        }

        if (critical && !newExceptions.isEmpty()) {
            prerequisiteExceptions.addAll(newExceptions);
            Exception cause = prerequisiteExceptions.stream()
                .reduce((primary, current) -> {
                    primary.addSuppressed(current);
                    return primary;
                }).get();
            throw new DeprovisioningException("Critical error when deprovisioning resources", cause);
        }
        return newExceptions;
    }
    
}

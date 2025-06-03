package com.mc_host.api.service.stripe.events;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.resource.hetzner.HetznerNode;
import com.mc_host.api.model.stripe.MetadataKey;
import com.mc_host.api.model.stripe.StripeEventType;
import com.mc_host.api.model.stripe.SubscriptionStatus;
import com.mc_host.api.model.subscription.ContentSubscription;
import com.mc_host.api.model.subscription.MarketingRegion;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.repository.PlanRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.Mode;
import com.mc_host.api.service.resources.v2.context.Status;
import com.mc_host.api.service.resources.v2.context.StepType;
import com.mc_host.api.service.resources.v2.service.ServerExecutor;
import com.mc_host.api.util.PersistenceContext;
import com.mc_host.api.util.Task;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;

@Service
public class StripeSubscriptionService implements StripeEventService {
    private static final Logger LOGGER = Logger.getLogger(StripeSubscriptionService.class.getName());

    private final StripeEventProcessor stripeEventProcessor;
    private final ServerExecutor serverExecutor;
    private final ScheduledExecutorService delayedTaskScheduler;
    private final SubscriptionRepository subscriptionRepository;
    private final ServerExecutionContextRepository serverExecutionContextRepository;
    private final PlanRepository planRepository;
    private final NodeRepository nodeRepository;
    private final PersistenceContext persistenceContext;

    public StripeSubscriptionService(
        StripeEventProcessor stripeEventProcessor,
        ServerExecutor serverExecutor,
        ScheduledExecutorService delayedTaskScheduler,
        SubscriptionRepository subscriptionRepository,
        ServerExecutionContextRepository serverExecutionContextRepository,
        PlanRepository planRepository,
        NodeRepository nodeRepository,
        PersistenceContext persistenceContext
    ) {
        this.stripeEventProcessor = stripeEventProcessor;
        this.serverExecutor = serverExecutor;
        this.delayedTaskScheduler = delayedTaskScheduler;
        this.subscriptionRepository = subscriptionRepository;
        this.serverExecutionContextRepository = serverExecutionContextRepository;
        this.planRepository = planRepository;
        this.nodeRepository = nodeRepository;
        this.persistenceContext = persistenceContext;
    }

    public StripeEventType getType() {
        return StripeEventType.SUBSCRIPTION;
    }

    public void process(String customerId) {
        try {
            List<ContentSubscription> stripeSubscriptions = Subscription.list(Map.of("customer", customerId, "status", "all")).getData().stream()
                .map(subscription -> stripeSubscriptionToEntity(subscription, customerId)).toList();

            stripeSubscriptions.forEach(subscription -> persistenceContext.inTransaction(() -> {
                // update subscription details, and add an execution context if subscription is new
                String specificationId = planRepository.selectSpecificationId(subscription.priceId())
                    .orElseThrow(() -> new IllegalStateException("No specification for price %s " + subscription.priceId()));
                subscriptionRepository.upsertSubscription(subscription);
                serverExecutionContextRepository.insertOrIgnoreSubscription(
                    Context.newIdle(
                        subscription.subscriptionId(), 
                        subscription.initialRegion(), 
                        specificationId, 
                        "My New Server", 
                        "A Minecraft Server"
                    )
                );

                // make sure context has up to date spec details
                Context context = serverExecutionContextRepository.selectSubscription(subscription.subscriptionId())
                    .orElseThrow(() -> new IllegalStateException("Context not found for subscription: " + subscription.subscriptionId()));
                serverExecutionContextRepository.upsertSubscription(context);
            }));

            List<CompletableFuture<Void>> tasks = stripeSubscriptions.stream()
                .map(pair -> Task.alwaysAttempt(
                    "Updating subscription",
                    () -> processSubscription(pair)
                )).toList();
            Task.awaitCompletion(tasks);
                
            CompletableFuture<Void> updateCurrency = Task.criticalTask(
                "Update user currency for " + customerId,
                () -> subscriptionRepository.updateUserCurrencyFromSubscription(customerId)
            );
            Task.awaitCompletion(updateCurrency);

            LOGGER.log(Level.INFO, "Executed subscription db sync for customer: " + customerId);
        } catch (StripeException e) {
            LOGGER.log(Level.SEVERE, "Failed to sync subscription data for customer: " + customerId, e);
            throw new RuntimeException("Failed to sync subscription data", e);
        }
    }

    private void processSubscription(ContentSubscription subscription) {

        Context context = serverExecutionContextRepository.selectSubscription(subscription.subscriptionId())
            .orElseThrow(() -> new IllegalStateException("Context not found for subscription: " + subscription.subscriptionId()));

        // let current provisioning finish
        if (context.getStatus().equals(Status.IN_PROGRESS)) {
            scheduleEnqueue(subscription, 30);
            return;
        }

        // retry failed provisioning
        if (context.getStatus().equals(Status.FAILED)) {
            serverExecutor.execute(context.inProgress());
            scheduleEnqueue(subscription, 180);
            return;
        }

        // if provisioning is not 'FAILED' or 'IN_PROGRESS', it must be 'COMPLETE'
        // a subscription shouldn't ever be 'COMPLETE' outside of a terminal step
        if (!context.isTerminal()) {
            throw new IllegalStateException(String.format("Subscription %s completed outside a terminal step", subscription.subscriptionId()));
        }

        // inactive subscription states (to 'DESTROY')
        if (subscription.status().isTerminated() || subscription.status().isPending() || subscription.status().equals(SubscriptionStatus.UNPAID)) {  
            if (context.isCreated()) {
                //TODO: back-up data
                serverExecutor.execute(context.inProgress().withMode(Mode.DESTROY));
            }
            return;
        }

        // active subscription states (to 'CREATE' or 'MIGRATE_CREATE')
        if (subscription.status().isActive() || subscription.status().equals(SubscriptionStatus.PAST_DUE)) {
            if (context.isCreated()) {
                // what is the actual spec / region that is provisioned
                HetznerNode hetznerNode = nodeRepository.selectHetznerNode(context.getNodeId())    
                    .orElseThrow(() -> new IllegalStateException(String.format("No node could be found for id: %s", context.getNodeId())));
                MarketingRegion groundRegion = hetznerNode.hetznerRegion().getMarketingRegion();
                String groundSpec = context.getSpecificationId(); // TODO: Determine the real spec of the server

                // if they do not match the desired state, and the server is active, execute a migration
                Boolean regionChanged = !context.getRegion().equals(groundRegion);
                Boolean specificationChanged = !context.getSpecificationId().equals(groundSpec);
                if (specificationChanged || regionChanged) {
                    serverExecutor.execute(context.inProgress().withMode(Mode.MIGRATE_CREATE).withStepType(StepType.NEW));
                }
            } else if (context.isDestroyed()) {
                // else create with new desired state
                serverExecutor.execute(context.inProgress().withMode(Mode.CREATE));
            }
            return;
        } 

        throw new IllegalStateException(String.format("Subscription %s failed all conditions", subscription.subscriptionId()));
    }

    private void scheduleEnqueue(ContentSubscription subscription, Integer delay) {
        stripeEventProcessor.scheduledEventRetry(getType(), subscription.customerId(), Duration.ofSeconds(30));
    }

    private ContentSubscription stripeSubscriptionToEntity(Subscription subscription, String customerId) {
        return new ContentSubscription(
            subscription.getId(), 
            customerId, 
            SubscriptionStatus.fromString(subscription.getStatus()), 
            subscription.getItems().getData().getFirst().getPrice().getId(), 
            Instant.ofEpochMilli(subscription.getCurrentPeriodEnd()), 
            Instant.ofEpochMilli(subscription.getCurrentPeriodStart()), 
            subscription.getCancelAtPeriodEnd(),
            MarketingRegion.valueOf(subscription.getMetadata().get(MetadataKey.REGION.name()))
        );
    }
}

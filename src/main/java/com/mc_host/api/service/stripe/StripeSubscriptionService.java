package com.mc_host.api.service.stripe;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.MarketingRegion;
import com.mc_host.api.model.MetadataKey;
import com.mc_host.api.model.SubscriptionStatus;
import com.mc_host.api.model.cache.StripeEventType;
import com.mc_host.api.model.entity.ContentSubscription;
import com.mc_host.api.model.entity.SubscriptionPair;
import com.mc_host.api.model.hetzner.HetznerRegion;
import com.mc_host.api.repository.PlanRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.service.resources.HetznerService;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.Mode;
import com.mc_host.api.service.resources.v2.context.Status;
import com.mc_host.api.service.resources.v2.context.StepType;
import com.mc_host.api.service.resources.v2.service.ServerExecutor;
import com.mc_host.api.util.Task;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;

@Service
public class StripeSubscriptionService implements StripeEventService {
    private static final Logger LOGGER = Logger.getLogger(StripeSubscriptionService.class.getName());

    private final ServerExecutor serverExecutor;
    private final SubscriptionRepository subscriptionRepository;
    private final ServerExecutionContextRepository serverExecutionContextRepository;
    private final PlanRepository planRepository;
    private final HetznerService hetznerService;

    public StripeSubscriptionService(
        ServerExecutor serverExecutor,
        SubscriptionRepository subscriptionRepository,
        ServerExecutionContextRepository serverExecutionContextRepository,
        PlanRepository planRepository,
        HetznerService hetznerService
    ) {
        this.serverExecutor = serverExecutor;
        this.subscriptionRepository = subscriptionRepository;
        this.serverExecutionContextRepository = serverExecutionContextRepository;
        this.planRepository = planRepository;
        this.hetznerService = hetznerService;
    }

    public StripeEventType getType() {
        return StripeEventType.SUBSCRIPTION;
    }

    public void process(String customerId) {
        try {
            List<ContentSubscription> dbSubscriptions = subscriptionRepository.selectSubscriptionsByCustomerId(customerId);
            List<ContentSubscription> stripeSubscriptions = Subscription.list(Map.of("customer", customerId)).getData().stream()
                .map(subscription -> stripeSubscriptionToEntity(subscription, customerId)).toList();

            List<SubscriptionPair> allPairs = Stream.concat(
            dbSubscriptions.stream()
                .map(dbSub -> {
                    ContentSubscription matchingStripe = stripeSubscriptions.stream()
                        .filter(dbSub::isAlike)
                        .findFirst()
                        .orElse(null);
                    return new SubscriptionPair(dbSub, matchingStripe);
                }),
            stripeSubscriptions.stream()
                .filter(stripeSub -> dbSubscriptions.stream().noneMatch(stripeSub::isAlike))
                .map(stripeSub -> new SubscriptionPair(null, stripeSub))
            ).toList();

            List<CompletableFuture<Void>> tasks = allPairs.stream()
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

    private void processSubscription(SubscriptionPair pair) {
        if(!pair.isValid()) {
            throw new IllegalStateException("No subscriptions in pair");
        }

        ContentSubscription newSubscription = pair.newSubscription();
        ContentSubscription oldSubscription = pair.oldSubscription();

        if (pair.isNew()) {
            String newSpecificationId = planRepository.selectSpecificationId(newSubscription.priceId())
                .orElseThrow(() -> new IllegalStateException("No specification for price %s " + newSubscription.priceId()));
            subscriptionRepository.insertSubscription(newSubscription);
            serverExecutor.execute(
                Context.create(newSubscription.subscriptionId(), Mode.CREATE, newSubscription.initialRegion(), newSpecificationId, "My New Server", "A Minecraft Server")
            );
            return;
        }

        Context context = serverExecutionContextRepository.selectSubscription(oldSubscription.subscriptionId())
            .orElseThrow(() -> new IllegalStateException("Context not found for subscription: " + oldSubscription.subscriptionId()));
        if (context.getStatus().equals(Status.IN_PROGRESS)) {
            //TODO: schedule requeue
            return;
        }

        if (context.getStatus().equals(Status.FAILED)) {
            serverExecutor.execute(context.inProgress());
            if (context.getMode().equals(Mode.DESTROY)) {
                subscriptionRepository.deleteSubscription(oldSubscription.subscriptionId());
            } else {
                //TODO: schedule requeue
            }
            return;
        }

        if (pair.isOld()) {
            serverExecutor.execute(context.inProgress().withMode(Mode.DESTROY));
            subscriptionRepository.deleteSubscription(oldSubscription.subscriptionId());
            return;
        }

        SubscriptionStatus newSubscriptionStatus = SubscriptionStatus.fromStripeValue(newSubscription.status());
        if (newSubscriptionStatus.isTerminated()) {
            //TODO: back-up data
            serverExecutor.execute(context.inProgress().withMode(Mode.DESTROY));
            subscriptionRepository.deleteSubscription(oldSubscription.subscriptionId());
            return; 
        }

        if (newSubscriptionStatus.isPending() || newSubscriptionStatus.equals(SubscriptionStatus.UNPAID)) {
            //TODO: back-up data
            subscriptionRepository.updateSubscription(newSubscription);
            serverExecutor.execute(context.inProgress().withMode(Mode.DESTROY));
            return;
        }

        if (newSubscriptionStatus.isActive() || newSubscriptionStatus.equals(SubscriptionStatus.PAST_DUE)) {
            //TODO: back-up data
            serverExecutor.execute(context.inProgress().withMode(Mode.CREATE));

            Boolean priceChanged = !newSubscription.priceId().equals(oldSubscription.priceId());
            HetznerRegion actualRegion = hetznerService.getServerRegion(hetznerService.getNodeId(newSubscription.subscriptionId()));
            Boolean regionChanged = !context.getRegion().equals(actualRegion.getMarketingRegion());

            if (priceChanged || regionChanged) {
                serverExecutor.execute(context.inProgress().withMode(Mode.MIGRATE_CREATE).withStepType(StepType.NEW));
            }
            subscriptionRepository.updateSubscription(newSubscription);
            return;
        } 

        // should be unreachable
        throw new IllegalStateException(String.format("Subscription %s failed all conditions"));

    }

    private ContentSubscription stripeSubscriptionToEntity(Subscription subscription, String customerId) {
        return new ContentSubscription(
            subscription.getId(), 
            customerId, 
            subscription.getStatus(), 
            subscription.getItems().getData().getFirst().getPrice().getId(), 
            Instant.ofEpochMilli(subscription.getCurrentPeriodEnd()), 
            Instant.ofEpochMilli(subscription.getCurrentPeriodStart()), 
            subscription.getCancelAtPeriodEnd(),
            MarketingRegion.valueOf(subscription.getMetadata().get(MetadataKey.REGION.name()))
        );
    }
}

package com.mc_host.api.queue.processor;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.Mode;
import com.mc_host.api.model.provisioning.Status;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.queue.Job;
import com.mc_host.api.model.queue.JobType;
import com.mc_host.api.model.resource.hetzner.HetznerNode;
import com.mc_host.api.model.resource.pterodactyl.PterodactylServer;
import com.mc_host.api.model.stripe.SubscriptionStatus;
import com.mc_host.api.model.subscription.ContentSubscription;
import com.mc_host.api.model.subscription.MarketingRegion;
import com.mc_host.api.queue.JobScheduler;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.repository.PlanRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.service.provisioning.ServerExecutor;
import com.mc_host.api.util.PersistenceContext;
import com.mc_host.api.util.Task;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@RequiredArgsConstructor
public class CustomerSubscriptionSyncJobProcessor implements JobProcessor {
	private static final Logger LOGGER = Logger.getLogger(CustomerSubscriptionSyncJobProcessor.class.getName());

	private final JobScheduler jobScheduler;
	private final ServerExecutor serverExecutor;
	private final SubscriptionRepository subscriptionRepository;
	private final ServerExecutionContextRepository serverExecutionContextRepository;
	private final PlanRepository planRepository;
	private final NodeRepository nodeRepository;
	private final GameServerRepository gameServerRepository;
	private final PersistenceContext persistenceContext;

	@Override
	public JobType getJobType() {
		return JobType.PER_SUBSCRIPTION_SYNC;
	}

	@Override
	public void process(Job job) throws Exception {
		LOGGER.info("Processing %s job: %s".formatted(getJobType(), job.jobId()));
		process(job.payload());
		LOGGER.info("%s job completed for: %s".formatted(getJobType(), job.jobId()));
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
						"My New Server",
						"A Minecraft Server"
					)
				);

				// make sure context has up-to-date spec details
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

			subscriptionRepository.updateUserCurrencyFromSubscription(customerId);

			LOGGER.log(Level.FINE, "Executed subscription db sync for customer: " + customerId);
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
			scheduleEnqueue(subscription, 10);
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

		if (subscription.status().isActive() || subscription.status().equals(SubscriptionStatus.PAST_DUE)) {
			if (context.isCreated()) {
				// what is the actual spec that is provisioned
				String specificationId = planRepository.selectSpecificationId(subscription.priceId())
					.orElseThrow(() -> new IllegalStateException(String.format("No specification could be found for price: %s", subscription.priceId())));
				HetznerNode hetznerNode = nodeRepository.selectHetznerNode(context.getNodeId())
					.orElseThrow(() -> new IllegalStateException(String.format("No node could be found for id: %s", context.getNodeId())));

				// check if spec has changed
				if (specificationId.equals(hetznerNode.hetznerSpec().getSpecificationId())) {
					// check serverKey, initiate a migration if changed
					String serverKey = gameServerRepository.selectPterodactylServer(context.getPterodactylServerId())
						.map(PterodactylServer::serverKey)
						.orElseThrow(() -> new IllegalStateException(String.format("No pterodactyl server could be found for id: %s", context.getPterodactylServerId())));

					if (serverKey.equals(context.getServerKey())) {
						return; // nothing to do
					}
				}

				// something needs migration
				serverExecutor.execute(context.inProgress().withMode(Mode.MIGRATE_CREATE).withStepType(StepType.NEW));
			} else if (context.isDestroyed()) {
				serverExecutor.execute(context.inProgress().withMode(Mode.CREATE));
			}
			return;
		}

		throw new IllegalStateException(String.format("Subscription %s failed all conditions", subscription.subscriptionId()));
	}

	private void scheduleEnqueue(ContentSubscription subscription, Integer delay) {
		jobScheduler.scheduleSubscriptionSync(subscription.customerId(), delay);
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
			MarketingRegion.WEST_EUROPE
		);
	}
}

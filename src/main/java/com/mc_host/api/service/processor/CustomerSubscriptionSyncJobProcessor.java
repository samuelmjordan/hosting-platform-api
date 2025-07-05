package com.mc_host.api.service.processor;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.queue.Job;
import com.mc_host.api.model.queue.JobType;
import com.mc_host.api.model.stripe.SubscriptionStatus;
import com.mc_host.api.model.subscription.ContentSubscription;
import com.mc_host.api.queue.JobScheduler;
import com.mc_host.api.queue.processor.JobProcessor;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.service.FakerService;
import com.mc_host.api.util.PersistenceContext;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@RequiredArgsConstructor
public class CustomerSubscriptionSyncJobProcessor implements JobProcessor {
	private static final Logger LOGGER = Logger.getLogger(CustomerSubscriptionSyncJobProcessor.class.getName());

	private final JobScheduler jobScheduler;
	private final FakerService fakerService;
	private final SubscriptionRepository subscriptionRepository;
	private final ServerExecutionContextRepository serverExecutionContextRepository;
	private final PersistenceContext persistenceContext;

	@Override
	public JobType getJobType() {
		return JobType.CUSTOMER_SUBSCRIPTION_SYNC;
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
				.map(subscription -> stripeSubscriptionToNewEntity(subscription, customerId)).toList();

			stripeSubscriptions.forEach(subscription -> persistenceContext.inTransaction(() -> {
				// update subscription details, and add an execution context if subscription is new
				subscriptionRepository.upsertSubscription(subscription);
				serverExecutionContextRepository.insertOrIgnoreSubscription(
					Context.newIdle(
						subscription.subscriptionId(),
						"My New Server",
						"A Minecraft Server"
					)
				);
			}));

			stripeSubscriptions
				.stream()
				.map(ContentSubscription::subscriptionId)
				.forEach(jobScheduler::scheduleSubscriptionSync);

			subscriptionRepository.updateUserCurrencyFromSubscription(customerId);

			LOGGER.log(Level.FINE, "Executed subscription db sync for customer: " + customerId);
		} catch (StripeException e) {
			LOGGER.log(Level.SEVERE, "Failed to sync subscription data for customer: " + customerId, e);
			throw new RuntimeException("Failed to sync subscription data", e);
		}
	}

	private ContentSubscription stripeSubscriptionToNewEntity(Subscription subscription, String customerId) {
		return new ContentSubscription(
			subscription.getId(),
			customerId,
			SubscriptionStatus.fromString(subscription.getStatus()),
			subscription.getItems().getData().getFirst().getPrice().getId(),
			Instant.ofEpochMilli(subscription.getCurrentPeriodEnd()),
			Instant.ofEpochMilli(subscription.getCurrentPeriodStart()),
			subscription.getCancelAtPeriodEnd(),
			fakerService.generateSubdomain()
		);
	}
}

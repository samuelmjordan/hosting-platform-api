package com.mc_host.api.queue.v2.service.processor;

import com.mc_host.api.queue.v2.model.Job;
import com.mc_host.api.queue.v2.model.JobType;
import com.mc_host.api.service.stripe.events.StripeSubscriptionService;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class SubscriptionSyncJobProcessor implements JobProcessor {
	private static final Logger LOGGER = Logger.getLogger(SubscriptionSyncJobProcessor.class.getName());

	private final StripeSubscriptionService stripeSubscriptionService;

	public SubscriptionSyncJobProcessor(
		StripeSubscriptionService stripeSubscriptionService
	) {
		this.stripeSubscriptionService = stripeSubscriptionService;
	}

	@Override
	public JobType getJobType() {
		return JobType.CUSTOMER_SUBSCRIPTION_SYNC;
	}

	@Override
	public void process(Job job) throws Exception {
		LOGGER.info("processing subscription sync job: %s".formatted(job.jobId()));
		LOGGER.info("syncing subscription: %s".formatted(job.payload()));

		stripeSubscriptionService.process(job.payload());

		LOGGER.info("subscription sync completed for: %s".formatted(job.payload()));
	}
}

package com.mc_host.api.queue.service.processor;

import com.mc_host.api.queue.model.Job;
import com.mc_host.api.queue.model.JobType;
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
		LOGGER.info("Processing %s job: %s".formatted(getJobType(), job.jobId()));

		stripeSubscriptionService.process(job.payload());

		LOGGER.info("%s job completed for: %s".formatted(getJobType(), job.jobId()));
	}
}

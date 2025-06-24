package com.mc_host.api.queue.processor;

import com.mc_host.api.model.queue.Job;
import com.mc_host.api.model.queue.JobType;
import com.mc_host.api.service.stripe.events.StripeSubscriptionService;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class CustomerSubscriptionSyncJobProcessor implements JobProcessor {
	private static final Logger LOGGER = Logger.getLogger(CustomerSubscriptionSyncJobProcessor.class.getName());

	private final StripeSubscriptionService stripeSubscriptionService;

	public CustomerSubscriptionSyncJobProcessor(
		StripeSubscriptionService stripeSubscriptionService
	) {
		this.stripeSubscriptionService = stripeSubscriptionService;
	}

	@Override
	public JobType getJobType() {
		return JobType.PER_SUBSCRIPTION_SYNC;
	}

	@Override
	public void process(Job job) throws Exception {
		LOGGER.info("Processing %s job: %s".formatted(getJobType(), job.jobId()));

		stripeSubscriptionService.process(job.payload());

		LOGGER.info("%s job completed for: %s".formatted(getJobType(), job.jobId()));
	}
}

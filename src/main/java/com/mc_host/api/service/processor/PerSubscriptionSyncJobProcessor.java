package com.mc_host.api.service.processor;

import com.mc_host.api.model.queue.Job;
import com.mc_host.api.model.queue.JobType;
import com.mc_host.api.queue.processor.JobProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
@RequiredArgsConstructor
public class PerSubscriptionSyncJobProcessor implements JobProcessor {
	private static final Logger LOGGER = Logger.getLogger(PerSubscriptionSyncJobProcessor.class.getName());

	@Override
	public JobType getJobType() {
		return JobType.CUSTOMER_SUBSCRIPTION_SYNC;
	}

	@Override
	public void process(Job job) throws Exception {
		LOGGER.info("Processing %s job: %s".formatted(getJobType(), job.jobId()));

		//stripeSubscriptionService.process(job.payload());

		LOGGER.info("%s job completed for: %s".formatted(getJobType(), job.jobId()));
	}
}

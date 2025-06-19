package com.mc_host.api.queue.v2.service.processor;

import com.mc_host.api.queue.v2.model.Job;
import com.mc_host.api.queue.v2.model.JobType;
import com.mc_host.api.service.stripe.events.StripePriceService;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class PriceSyncJobProcessor implements JobProcessor {
	private static final Logger LOGGER = Logger.getLogger(PriceSyncJobProcessor.class.getName());

	private final StripePriceService stripePriceService;

	public PriceSyncJobProcessor(
		StripePriceService stripePriceService
	) {
		this.stripePriceService = stripePriceService;
	}

	@Override
	public JobType getJobType() {
		return JobType.PRODUCT_PRICE_SYNC;
	}

	@Override
	public void process(Job job) throws Exception {
		LOGGER.info("Processing %s job: %s".formatted(getJobType(), job.jobId()));

		stripePriceService.process(job.payload());

		LOGGER.info("%s job completed for: %s".formatted(getJobType(), job.jobId()));
	}
}

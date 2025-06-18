package com.mc_host.api.queue.v2.service.processor;

import com.mc_host.api.queue.v2.model.Job;
import com.mc_host.api.queue.v2.model.JobType;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class PriceSyncJobProcessor implements JobProcessor {
	private static final Logger LOGGER = Logger.getLogger(PriceSyncJobProcessor.class.getName());

	@Override
	public JobType getJobType() {
		return JobType.PRICE_SYNC;
	}

	@Override
	public void process(Job job) throws Exception {
		LOGGER.info("processing price sync job: %s".formatted(job.jobId()));

		LOGGER.info("syncing price: %s".formatted(job.payload()));

		// work

		LOGGER.info("price sync completed for: %s".formatted(job.payload()));
	}
}

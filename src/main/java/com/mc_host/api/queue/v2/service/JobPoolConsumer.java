package com.mc_host.api.queue.v2.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class JobPoolConsumer {
	private static final Logger LOGGER = Logger.getLogger(JobPoolConsumer.class.getName());

	private final JobPoolService jobPoolService;

	public JobPoolConsumer(
		JobPoolService jobPoolService
	) {
		this.jobPoolService = jobPoolService;
	}

	@Scheduled(fixedDelay = 2000) // every 2 seconds
	public void processJobs() {
		jobPoolService.processJobs();
	}
}

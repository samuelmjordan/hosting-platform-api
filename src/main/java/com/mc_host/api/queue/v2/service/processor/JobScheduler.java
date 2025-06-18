package com.mc_host.api.queue.v2.service.processor;

import com.mc_host.api.queue.v2.service.JobPoolService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class JobScheduler {
	private static final Logger LOGGER = Logger.getLogger(JobScheduler.class.getName());

	private final JobPoolService jobPoolService;

	public JobScheduler(JobPoolService jobPoolService) {
		this.jobPoolService = jobPoolService;
	}

	@Scheduled(fixedDelay = 5000) // every 5 seconds
	public void processJobs() {
		jobPoolService.processJobs();
	}

}

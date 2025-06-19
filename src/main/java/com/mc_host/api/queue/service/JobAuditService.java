package com.mc_host.api.queue.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.logging.Logger;

@Service
public class JobAuditService {
	private static final Logger LOGGER = Logger.getLogger(JobAuditService.class.getName());

	private final JobRepository jobRepository;

	public JobAuditService(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	@Scheduled(fixedRate = 60000)
	@Transactional
	public void archiveCompletedJobs() {
		try {
			boolean lockAcquired = jobRepository.tryAcquireCleanupLock();
			if (!lockAcquired) {
				return;
			}

			try {
				int processed = jobRepository.archiveAndDeleteCompletedJobs();

				if (processed > 0) {
					LOGGER.info("Archived and deleted %d completed jobs".formatted(processed));
				}
			} finally {
				jobRepository.releaseCleanupLock();
			}

		} catch (Exception e) {
			LOGGER.severe("failed to archive jobs: " + e.getMessage());
		}
	}
}
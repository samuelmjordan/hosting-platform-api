package com.mc_host.api.queue.v2.service;

import com.mc_host.api.queue.v2.model.Job;
import com.mc_host.api.queue.v2.model.JobStatus;
import com.mc_host.api.queue.v2.model.JobType;
import com.mc_host.api.queue.v2.service.processor.JobProcessor;
import com.mc_host.api.queue.v2.service.processor.JobProcessorFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

@Service
@Transactional
public class JobPoolService {
	private static final Logger LOGGER = Logger.getLogger(JobPoolService.class.getName());

	private final JobRepository jobRepository;
	private final JobProcessorFactory processorFactory;
	private final ThreadPoolExecutor threadPoolExecutor;

	public JobPoolService(
		JobRepository jobRepository,
		JobProcessorFactory processorFactory,
		ThreadPoolExecutor threadPoolExecutor
	) {
		this.jobRepository = jobRepository;
		this.processorFactory = processorFactory;
		this.threadPoolExecutor = threadPoolExecutor;
	}

	public void enqueue(JobType type, String payload) {
		enqueue(type, payload, Instant.now());
	}

	public void enqueue(JobType type, String payload, Instant delayedUntil) {
		enqueue(type, payload, delayedUntil, 3);
	}

	public void enqueue(JobType type, String payload, Instant delayedUntil, Integer maxRetries) {
		try {
			processorFactory.getProcessor(type);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("no processor found for job type: " + type);
		}

		String dedupKey = String.join("::", type.name(), payload);

		Optional<Job> existingJob = jobRepository.findDuplicateJob(type, dedupKey);
		if (existingJob.isPresent()) {
			jobRepository.mergeJobKeepingEarliestSchedule(existingJob.get().jobId(), payload, delayedUntil);
			return;
		}

		String jobId = UUID.randomUUID().toString();
		Job job = new Job(
			jobId,
			dedupKey,
			type,
			JobStatus.PENDING,
			payload,
			0,
			maxRetries,
			null,
			delayedUntil
		);

		jobRepository.insertJob(job);
		LOGGER.info("enqueued new job: %s of type: %s".formatted(jobId, type));
	}

	public void processJobs() {
		try {
			int available = threadPoolExecutor.getMaximumPoolSize() - threadPoolExecutor.getActiveCount();
			List<Job> pendingJobs = jobRepository.claimJobsByStatus((int) Math.ceil(available * 0.8), JobStatus.PENDING);
			List<Job> retryingJobs = jobRepository.claimJobsByStatus((int) Math.ceil(available * 0.3), JobStatus.RETRYING);

			int claimed = pendingJobs.size() + retryingJobs.size();
			if (claimed > 0) {
				LOGGER.info("Claimed %s jobs to process".formatted(pendingJobs.size() + retryingJobs.size()));
			}

			Stream.concat(pendingJobs.stream(), retryingJobs.stream())
				.forEach(job -> threadPoolExecutor.submit(() -> processJob(job)));
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "error claiming jobs", e);
		}
	}

	private void processJob(Job job) {
		JobProcessor processor;
		try {
			processor = processorFactory.getProcessor(job.type());
		} catch (IllegalArgumentException e) {
			LOGGER.log(Level.SEVERE, "no processor found for job type: {}", job.type());
			handleJobFailure(job, e);
			return;
		}

		Optional<Job> processingJob = jobRepository.findActiveDuplicateJobs(job.type(), job.dedupKey());
		if (processingJob.isPresent()) {
			handleJobCollision(job);
		}

		try {
			processor.process(job);
			jobRepository.updateJobStatus(job.jobId(), JobStatus.COMPLETED, null);
		} catch (Exception e) {
			handleJobFailure(job, e);
		}
	}

	private void handleJobFailure(Job job, Exception e) {
		int newRetryCount = job.retryCount() + 1;

		if (newRetryCount >= job.maximumRetries()) {
			jobRepository.moveToDeadLetter(job.jobId(), e.getMessage());
			LOGGER.log(Level.SEVERE, "job moved to dead letter: %s after %s attempts".formatted(job.jobId(), newRetryCount));
		} else {
			long delaySeconds = (long) Math.max(Math.pow(2, newRetryCount), 30);
			Instant retryAt = Instant.now().plus(Duration.ofSeconds(delaySeconds));

			jobRepository.updateJobForRetry(job.jobId(), newRetryCount, retryAt, e.getMessage());
			LOGGER.log(Level.SEVERE, "job scheduled for retry: %s attempt %s in %s seconds"
				.formatted(job.jobId(), newRetryCount, delaySeconds));
		}
	}

	private void handleJobCollision(Job job) {
		jobRepository.updateJobForNonFailureRetry(job.jobId(), Instant.now().plus(Duration.ofSeconds(30)));
	}
}
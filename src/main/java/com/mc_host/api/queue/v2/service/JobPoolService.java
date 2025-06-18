package com.mc_host.api.queue.v2.service;

import com.mc_host.api.queue.v2.model.Job;
import com.mc_host.api.queue.v2.model.JobStatus;
import com.mc_host.api.queue.v2.model.JobType;
import com.mc_host.api.queue.v2.service.processor.JobProcessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class JobPoolService {
	private static final Logger LOGGER = Logger.getLogger(JobPoolService.class.getName());

	private final JobRepository jobRepository;
	private final Map<JobType, JobProcessor> processors;
	private final ThreadPoolExecutor threadPoolExecutor;

	public JobPoolService(
			JobRepository jobRepository,
			List<JobProcessor> processorList,
			ThreadPoolExecutor threadPoolExecutor
	) throws Exception {
		this.jobRepository = jobRepository;
		this.processors = processorList.stream()
				.collect(Collectors.toMap(JobProcessor::getJobType, p -> p));
		this.threadPoolExecutor = threadPoolExecutor;}

	public void enqueue(JobType type, String payload) {
		enqueue(type, payload, 3, Instant.now());
	}

	public void enqueue(JobType type, String payload, Integer maxRetries) {
		enqueue(type, payload, maxRetries, Instant.now());
	}

	public void enqueue(JobType type, String payload, Integer maxRetries, Instant delayedUntil) {
		if (!processors.containsKey(type)) {
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
		JobProcessor processor = processors.get(job.type());
		if (processor == null) {
			LOGGER.log(Level.SEVERE, "no processor found for job type: {}", job.type());
			jobRepository.updateJobStatus(job.jobId(), JobStatus.FAILED, "no processor available");
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

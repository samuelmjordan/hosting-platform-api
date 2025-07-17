package com.mc_host.api.queue;

import com.mc_host.api.model.queue.Job;
import com.mc_host.api.model.queue.JobStatus;
import com.mc_host.api.model.queue.JobType;
import com.mc_host.api.queue.processor.JobProcessor;
import com.mc_host.api.queue.processor.JobProcessorFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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

	public Job enqueue(JobType type, String payload) {
		return enqueue(type, payload, Instant.now());
	}

	public Job enqueue(JobType type, String payload, Instant delayedUntil) {
		return enqueue(type, payload, delayedUntil, 3);
	}

	public Job enqueue(JobType type, String payload, Instant delayedUntil, Integer maxRetries) {
		try {
			processorFactory.getProcessor(type);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("no processor found for job type: " + type);
		}

		String dedupKey = String.join("::", type.name(), payload);
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

		Job resultJob = jobRepository.upsertJob(job);

		boolean wasNewJob = resultJob.jobId().equals(jobId);
		if (wasNewJob) {
			LOGGER.info("enqueued new job: %s of type: %s".formatted(jobId, type));
		} else {
			LOGGER.log(Level.FINE, "merged duplicate job: %s (original: %s) of type: %s"
				.formatted(jobId, resultJob.jobId(), type));
		}

		return resultJob;
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

	void processJob(Job job) {
		JobProcessor processor;
		try {
			processor = processorFactory.getProcessor(job.type());
		} catch (IllegalArgumentException e) {
			LOGGER.log(Level.SEVERE, "no processor found for job type: {}", job.type());
			handleJobFailure(job, e);
			return;
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

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String fullTrace = sw.toString();

		LOGGER.severe("job %s failed: %s".formatted(job.jobId(), fullTrace));

		if (newRetryCount >= job.maximumRetries()) {
			jobRepository.moveToDeadLetter(job.jobId(), fullTrace);
			LOGGER.log(Level.SEVERE, "job moved to dead letter: %s after %s attempts".formatted(job.jobId(), newRetryCount));
		} else {
			long delaySeconds = (long) Math.max(Math.pow(2, newRetryCount), 30);
			Instant retryAt = Instant.now().plus(Duration.ofSeconds(delaySeconds));

			jobRepository.updateJobForRetry(job.jobId(), newRetryCount, retryAt, fullTrace);
			LOGGER.log(Level.SEVERE, "job scheduled for retry: %s attempt %s in %s seconds"
				.formatted(job.jobId(), newRetryCount, delaySeconds));
		}
	}
}
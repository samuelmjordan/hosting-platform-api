package com.mc_host.api.queue;

import com.mc_host.api.model.queue.Job;
import com.mc_host.api.model.queue.JobStatus;
import com.mc_host.api.model.queue.JobType;
import com.mc_host.api.queue.processor.JobProcessor;
import com.mc_host.api.queue.processor.JobProcessorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobPoolServiceTest {

	@Mock
	private JobRepository jobRepository;

	@Mock
	private JobProcessorFactory processorFactory;

	@Mock
	private ThreadPoolExecutor threadPoolExecutor;

	@Mock
	private JobProcessor jobProcessor;

	private JobPoolService jobPoolService;

	@BeforeEach
	void setUp() {
		jobPoolService = new JobPoolService(jobRepository, processorFactory, threadPoolExecutor);
	}

	@Test
	void enqueue_shouldCreateNewJob() {
		// given
		JobType jobType = JobType.CUSTOMER_SUBSCRIPTION_SYNC;
		String payload = "customer-123";
		Job expectedJob = createJob("job-id", jobType, payload);

		when(processorFactory.getProcessor(jobType)).thenReturn(jobProcessor);
		when(jobRepository.upsertJob(any(Job.class))).thenReturn(expectedJob);

		// when
		Job result = jobPoolService.enqueue(jobType, payload);

		// then
		assertThat(result).isEqualTo(expectedJob);
		verify(jobRepository).upsertJob(argThat(job ->
			job.type() == jobType &&
				job.payload().equals(payload) &&
				job.status() == JobStatus.PENDING
		));
	}

	@Test
	void enqueue_shouldThrowWhenNoProcessorFound() {
		// given
		JobType jobType = JobType.CUSTOMER_SUBSCRIPTION_SYNC;
		String payload = "customer-123";

		when(processorFactory.getProcessor(jobType))
			.thenThrow(new IllegalArgumentException("no processor"));

		// when/then
		assertThatThrownBy(() -> jobPoolService.enqueue(jobType, payload))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("no processor found for job type: " + jobType);
	}

	@Test
	void processJobs_shouldClaimAndSubmitJobs() {
		// given
		Job pendingJob = createJob("pending-1", JobType.CUSTOMER_SUBSCRIPTION_SYNC, "payload");
		Job retryJob = createJob("retry-1", JobType.PER_SUBSCRIPTION_SYNC, "payload");

		when(threadPoolExecutor.getMaximumPoolSize()).thenReturn(10);
		when(threadPoolExecutor.getActiveCount()).thenReturn(2);
		when(jobRepository.claimJobsByStatus(eq(7), eq(JobStatus.PENDING)))
			.thenReturn(List.of(pendingJob));
		when(jobRepository.claimJobsByStatus(eq(3), eq(JobStatus.RETRYING)))
			.thenReturn(List.of(retryJob));

		// when
		jobPoolService.processJobs();

		// then
		verify(threadPoolExecutor, times(2)).submit(any(Runnable.class));
	}

	@Test
	void processJob_shouldCompleteSuccessfully() throws Exception {
		// given
		Job job = createJob("job-1", JobType.CUSTOMER_SUBSCRIPTION_SYNC, "payload");

		when(processorFactory.getProcessor(job.type())).thenReturn(jobProcessor);
		doNothing().when(jobProcessor).process(job);

		// when
		jobPoolService.processJob(job);

		// then
		verify(jobProcessor).process(job);
		verify(jobRepository).updateJobStatus(job.jobId(), JobStatus.COMPLETED, null);
	}

	@Test
	void processJob_shouldRetryOnFailure() throws Exception {
		// given
		Job job = createJob("job-1", JobType.CUSTOMER_SUBSCRIPTION_SYNC, "payload", 0, 3);
		RuntimeException exception = new RuntimeException("processing failed");

		when(processorFactory.getProcessor(job.type())).thenReturn(jobProcessor);
		doThrow(exception).when(jobProcessor).process(job);

		// when
		jobPoolService.processJob(job);

		// then
		verify(jobRepository).updateJobForRetry(
			eq(job.jobId()),
			eq(1),
			any(Instant.class),
			contains("processing failed")
		);
	}

	@Test
	void processJob_shouldMoveToDeadLetterWhenMaxRetriesReached() throws Exception {
		// given
		Job job = createJob("job-1", JobType.CUSTOMER_SUBSCRIPTION_SYNC, "payload", 2, 3);
		RuntimeException exception = new RuntimeException("final failure");

		when(processorFactory.getProcessor(job.type())).thenReturn(jobProcessor);
		doThrow(exception).when(jobProcessor).process(job);

		// when
		jobPoolService.processJob(job);

		// then
		verify(jobRepository).moveToDeadLetter(
			eq(job.jobId()),
			contains("final failure")
		);
	}

	@Test
	void processJob_shouldHandleNoProcessorFound() {
		// given
		Job job = createJob("job-1", JobType.CUSTOMER_SUBSCRIPTION_SYNC, "payload");

		when(processorFactory.getProcessor(job.type()))
			.thenThrow(new IllegalArgumentException("no processor"));

		// when
		jobPoolService.processJob(job);
		jobPoolService.processJob(job);
		jobPoolService.processJob(job);

		// then
		verify(jobRepository).moveToDeadLetter(
			eq(job.jobId()),
			contains("no processor")
		);
	}

	private Job createJob(String jobId, JobType type, String payload) {
		return createJob(jobId, type, payload, 0, 3);
	}

	private Job createJob(String jobId, JobType type, String payload, int retryCount, int maxRetries) {
		return new Job(
			jobId,
			type.name() + "::" + payload,
			type,
			JobStatus.PENDING,
			payload,
			retryCount,
			maxRetries,
			null,
			Instant.now()
		);
	}
}
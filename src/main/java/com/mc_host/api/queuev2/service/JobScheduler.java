package com.mc_host.api.queuev2.service;

import com.mc_host.api.queuev2.model.JobType;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JobScheduler {
	private final JobPoolService jobPoolService;

	public JobScheduler(JobPoolService jobPoolService) {
		this.jobPoolService = jobPoolService;
	}

	public void schedule(JobType jobType, String payload, Integer delaySeconds) {
		jobPoolService.enqueue(jobType, payload, Instant.now().plusSeconds(delaySeconds));
	}

	public void schedule(JobType jobType, String payload) {
		schedule(jobType, payload, 0);
	}

	public void schedule(JobType jobType, String payload, Instant delayedUntil) {
		jobPoolService.enqueue(jobType, payload, delayedUntil);
	}

	public void schedule(JobType jobType, String payload, Instant delayedUntil, Integer maxRetries) {
		jobPoolService.enqueue(jobType, payload, delayedUntil, maxRetries);
	}

	// Convenience method
	public void scheduleSubscriptionSync(String customerId, Integer delaySeconds) {
		schedule(JobType.CUSTOMER_SUBSCRIPTION_SYNC, customerId, delaySeconds);
	}

	public void scheduleSubscriptionSync(String customerId) {
		scheduleSubscriptionSync(customerId, 0);
	}

	public void schedulePaymentMethodSync(String customerId, Integer delaySeconds) {
		schedule(JobType.CUSTOMER_PAYMENT_METHOD_SYNC, customerId, delaySeconds);
	}

	public void schedulePaymentMethodSync(String customerId) {
		schedulePaymentMethodSync(customerId, 0);
	}
}
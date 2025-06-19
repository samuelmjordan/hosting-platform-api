package com.mc_host.api.queue.model;

import java.time.Instant;

public record Job(
		String jobId,
		String dedupKey,
		JobType type,
		JobStatus status,
		String payload,
		Integer retryCount,
		Integer maximumRetries,
		String errorMessage,
		Instant delayedUntil
) {
}

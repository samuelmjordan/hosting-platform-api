package com.mc_host.api.model.queue;

public enum JobStatus {
	PENDING,
	PROCESSING,
	COMPLETED,
	FAILED,
	RETRYING,
	DEAD_LETTER
}

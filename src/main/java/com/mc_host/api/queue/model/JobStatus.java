package com.mc_host.api.queue.model;

public enum JobStatus {
	PENDING,
	PROCESSING,
	COMPLETED,
	FAILED,
	RETRYING,
	DEAD_LETTER
}

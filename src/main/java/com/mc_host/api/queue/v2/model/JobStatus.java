package com.mc_host.api.queue.v2.model;

public enum JobStatus {
	PENDING,
	PROCESSING,
	COMPLETED,
	FAILED,
	RETRYING,
	DEAD_LETTER
}

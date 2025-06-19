package com.mc_host.api.queuev2.model;

public enum JobStatus {
	PENDING,
	PROCESSING,
	COMPLETED,
	FAILED,
	RETRYING,
	DEAD_LETTER
}

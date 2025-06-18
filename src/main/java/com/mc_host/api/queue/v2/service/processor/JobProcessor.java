package com.mc_host.api.queue.v2.service.processor;

import com.mc_host.api.queue.v2.model.Job;
import com.mc_host.api.queue.v2.model.JobType;

public interface JobProcessor {
	void process(Job job) throws Exception;
	JobType getJobType();
}
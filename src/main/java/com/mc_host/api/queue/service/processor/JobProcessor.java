package com.mc_host.api.queue.service.processor;

import com.mc_host.api.queue.model.Job;
import com.mc_host.api.queue.model.JobType;

//TODO: should have customer level jobs and entity level jobs for stripe syncs
public interface JobProcessor {
	void process(Job job) throws Exception;
	JobType getJobType();
}
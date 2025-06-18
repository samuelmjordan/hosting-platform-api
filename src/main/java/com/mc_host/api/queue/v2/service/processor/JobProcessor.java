package com.mc_host.api.queue.v2.service.processor;

import com.mc_host.api.queue.v2.model.Job;
import com.mc_host.api.queue.v2.model.JobType;

//TODO: should have customer level jobs and entity level jobs for stripe syncs
public interface JobProcessor {
	void process(Job job) throws Exception;
	JobType getJobType();
}
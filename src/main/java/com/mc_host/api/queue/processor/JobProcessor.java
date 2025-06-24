package com.mc_host.api.queue.processor;

import com.mc_host.api.model.queue.Job;
import com.mc_host.api.model.queue.JobType;

//TODO: should have customer level jobs and entity level jobs for stripe syncs
public interface JobProcessor {
	void process(Job job) throws Exception;
	JobType getJobType();
}
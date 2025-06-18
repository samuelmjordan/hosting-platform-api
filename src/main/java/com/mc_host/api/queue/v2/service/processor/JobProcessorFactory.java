package com.mc_host.api.queue.v2.service.processor;

import com.mc_host.api.queue.v2.model.JobType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class JobProcessorFactory {
	private final ObjectProvider<JobProcessor> processorProvider;
	private final Map<JobType, JobProcessor> processorCache = new ConcurrentHashMap<>();

	public JobProcessorFactory(
		ObjectProvider<JobProcessor> processorProvider
	) {
		this.processorProvider = processorProvider;
	}

	public JobProcessor getProcessor(JobType jobType) {
		return processorCache.computeIfAbsent(jobType, this::discoverProcessor);
	}

	private JobProcessor discoverProcessor(JobType jobType) {
		return processorProvider.stream()
			.filter(processor -> processor.getJobType().equals(jobType))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("no processor found for job type: " + jobType));
	}

	public Map<JobType, String> getAvailableProcessors() {
		return processorProvider.stream()
			.collect(Collectors.toMap(
				JobProcessor::getJobType,
				processor -> processor.getClass().getSimpleName()
			));
	}
}
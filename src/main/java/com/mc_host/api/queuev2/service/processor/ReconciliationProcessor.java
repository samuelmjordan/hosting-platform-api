package com.mc_host.api.queuev2.service.processor;

import com.mc_host.api.model.resource.ResourceType;
import com.mc_host.api.queuev2.model.Job;
import com.mc_host.api.queuev2.model.JobType;
import com.mc_host.api.service.reconciliation.ResourceReconcilerSupplier;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class ReconciliationProcessor implements JobProcessor {
	private static final Logger LOGGER = Logger.getLogger(ReconciliationProcessor.class.getName());

	private final ResourceReconcilerSupplier resourceReconcilerSupplier;

	public ReconciliationProcessor(
		ResourceReconcilerSupplier resourceReconcilerSupplier
	) {
		this.resourceReconcilerSupplier = resourceReconcilerSupplier;
	}

	@Override
	public JobType getJobType() {
		return JobType.RECONCILE_RESOURCE_TYPE;
	}

	@Override
	public void process(Job job) throws Exception {
		LOGGER.info("Processing %s job: %s".formatted(getJobType(), job.jobId()));

		resourceReconcilerSupplier.supply(ResourceType.valueOf(job.payload())).reconcile();

		LOGGER.info("%s job completed for: %s".formatted(getJobType(), job.jobId()));
	}
}


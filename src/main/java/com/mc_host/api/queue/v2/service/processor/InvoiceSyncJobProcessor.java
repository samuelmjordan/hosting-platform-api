package com.mc_host.api.queue.v2.service.processor;

import com.mc_host.api.queue.v2.model.Job;
import com.mc_host.api.queue.v2.model.JobType;
import com.mc_host.api.service.stripe.events.StripeInvoiceService;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class InvoiceSyncJobProcessor implements JobProcessor {
	private static final Logger LOGGER = Logger.getLogger(InvoiceSyncJobProcessor.class.getName());

	private final StripeInvoiceService stripeInvoiceService;

	public InvoiceSyncJobProcessor(
		StripeInvoiceService stripeInvoiceService
	) {
		this.stripeInvoiceService = stripeInvoiceService;
	}

	@Override
	public JobType getJobType() {
		return JobType.CUSTOMER_INVOICE_SYNC;
	}

	@Override
	public void process(Job job) throws Exception {
		LOGGER.info("Processing %s job: %s".formatted(getJobType(), job.jobId()));

		stripeInvoiceService.process(job.payload());

		LOGGER.info("%s job completed for: %s".formatted(getJobType(), job.jobId()));
	}
}

package com.mc_host.api.queue.service.processor;

import com.mc_host.api.queue.model.Job;
import com.mc_host.api.queue.model.JobType;
import com.mc_host.api.service.stripe.events.StripePaymentMethodService;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class PaymentMethodSyncJobProcessor implements JobProcessor {
	private static final Logger LOGGER = Logger.getLogger(PaymentMethodSyncJobProcessor.class.getName());

	private final StripePaymentMethodService stripePaymentMethodService;

	public PaymentMethodSyncJobProcessor(
		StripePaymentMethodService stripePaymentMethodService
	) {
		this.stripePaymentMethodService = stripePaymentMethodService;
	}

	@Override
	public JobType getJobType() {
		return JobType.CUSTOMER_PAYMENT_METHOD_SYNC;
	}

	@Override
	public void process(Job job) throws Exception {
		LOGGER.info("Processing %s job: %s".formatted(getJobType(), job.jobId()));

		stripePaymentMethodService.process(job.payload());

		LOGGER.info("%s job completed for: %s".formatted(getJobType(), job.jobId()));
	}
}

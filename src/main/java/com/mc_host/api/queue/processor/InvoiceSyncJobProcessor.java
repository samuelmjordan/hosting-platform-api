package com.mc_host.api.queue.processor;

import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.queue.Job;
import com.mc_host.api.model.queue.JobType;
import com.mc_host.api.model.stripe.CustomerInvoice;
import com.mc_host.api.repository.InvoiceRepository;
import com.stripe.model.Invoice;
import com.stripe.param.InvoiceListParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;

@Component
@RequiredArgsConstructor
public class InvoiceSyncJobProcessor implements JobProcessor {
	private static final Logger LOGGER = Logger.getLogger(InvoiceSyncJobProcessor.class.getName());

	private final InvoiceRepository invoiceRepository;

	@Override
	public JobType getJobType() {
		return JobType.CUSTOMER_INVOICE_SYNC;
	}

	@Override
	public void process(Job job) throws Exception {
		LOGGER.info("Processing %s job: %s".formatted(getJobType(), job.jobId()));
		process(job.payload());
		LOGGER.info("%s job completed for: %s".formatted(getJobType(), job.jobId()));
	}

	@Transactional
	public void process(String customerId) {
		try {
			LOGGER.info("Syncing invoice data for customer: " + customerId);

			InvoiceListParams invoiceListParams = InvoiceListParams.builder()
				.setCustomer(customerId)
				.build();
			List<CustomerInvoice> stripeInvoices = Invoice.list(invoiceListParams).getData().stream()
				.map(invoice -> stripeInvoiceToEntity(invoice))
				.toList();

			LOGGER.info("Found " + stripeInvoices.size() + " invoices for customer: " + customerId);

			stripeInvoices.forEach(invoiceRepository::insertInvoice);
		} catch (Exception e) {
			LOGGER.severe("Error syncing invoice data for customer " + customerId + ": " + e.getMessage());
			throw new RuntimeException("Failed to sync invoice data", e);
		}
	}

	private CustomerInvoice stripeInvoiceToEntity(Invoice invoice) {
		return new CustomerInvoice(
			invoice.getId(),
			invoice.getCustomer(),
			invoice.getSubscription(),
			invoice.getNumber(),
			invoice.getPaid(),
			invoice.getDefaultPaymentMethod(),
			invoice.getCollectionMethod(),
			AcceptedCurrency.fromCode(invoice.getCurrency()),
			invoice.getAmountDue(),
			Instant.ofEpochMilli(invoice.getCreated()),
			invoice.getHostedInvoiceUrl()
		);
	}
}

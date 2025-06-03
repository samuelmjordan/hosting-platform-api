package com.mc_host.api.service.stripe.events;

import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.cache.StripeEventType;
import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.stripe.CustomerInvoice;
import com.mc_host.api.repository.InvoiceRepository;
import com.mc_host.api.util.Cache;
import com.stripe.model.Invoice;
import com.stripe.param.InvoiceListParams;

@Service
public class StripeInvoiceService implements StripeEventService{
    private static final Logger LOGGER = Logger.getLogger(StripeInvoiceService.class.getName());

    private final Cache cacheService;
    private final InvoiceRepository invoiceRepository;

    public StripeInvoiceService(
        Cache cacheService,
        InvoiceRepository invoiceRepository
    ) {
        this.cacheService = cacheService;
        this.invoiceRepository = invoiceRepository;
    }

    public StripeEventType getType() {
        return StripeEventType.INVOICE;
    }

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

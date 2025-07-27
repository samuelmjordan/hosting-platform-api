package com.mc_host.api.service;

import com.mc_host.api.configuration.ApplicationConfiguration;
import com.mc_host.api.controller.api.subscriptions.SubscriptionActionsController;
import com.mc_host.api.model.plan.ContentPrice;
import com.mc_host.api.model.stripe.request.UpdateSpecificationRequest;
import com.mc_host.api.model.subscription.ContentSubscription;
import com.mc_host.api.model.subscription.request.UpdateAddressRequest;
import com.mc_host.api.model.subscription.request.UpdateTitleRequest;
import com.mc_host.api.model.subscription.request.UpgradeConfirmationResponse;
import com.mc_host.api.model.subscription.request.UpgradePreviewResponse;
import com.mc_host.api.queue.JobScheduler;
import com.mc_host.api.repository.PlanRepository;
import com.mc_host.api.repository.PriceRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceLineItem;
import com.stripe.model.Subscription;
import com.stripe.param.InvoiceCreateParams;
import com.stripe.param.InvoiceUpcomingParams;
import com.stripe.param.SubscriptionUpdateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class SubscriptionActionsService implements SubscriptionActionsController {
    private final static Logger LOGGER = Logger.getLogger(SubscriptionActionsService.class.getName());

    private final PriceRepository priceRepository;
    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ServerExecutionContextRepository serverExecutionContextRepository;
    private final JobScheduler jobScheduler;
    private final ApplicationConfiguration applicationConfiguration;

    @Override
    public ResponseEntity<Void> cancelSubscription(String subscriptionId) {
        return updateCancelAtPeriodEnd(subscriptionId, true);
    }

    @Override
    public ResponseEntity<Void> uncancelSubscription(String subscriptionId) {
        return updateCancelAtPeriodEnd(subscriptionId, false);
    }

    @Override
    public ResponseEntity<UpgradePreviewResponse> previewSubscriptionSpecification(
        String subscriptionId,
        UpdateSpecificationRequest specificationRequest
    ) {
        try {
            ContentSubscription subscription = subscriptionRepository.selectSubscription(subscriptionId)
                .orElseThrow(() -> new IllegalStateException(String.format("Cannot find subscription %s", subscriptionId)));
            String oldPriceId = subscription.priceId();
            ContentPrice oldPrice = priceRepository.selectPrice(oldPriceId)
                .orElseThrow(() -> new IllegalStateException(String.format("Cannot find price %s", oldPriceId)));
            String newPriceId = planRepository.selectPriceIdFromSpecId(specificationRequest.specificationId())
                .orElseThrow(() -> new IllegalStateException(String.format("Cannot find a plan with specification %s and currency", specificationRequest.specificationId())));
            ContentPrice newPrice = priceRepository.selectPrice(newPriceId)
                .orElseThrow(() -> new IllegalStateException(String.format("Cannot find price %s", newPriceId)));

            if (!oldPrice.minorAmounts().containsKey(subscription.currency()) ||
                !newPrice.minorAmounts().containsKey(subscription.currency())
            ) {
                return ResponseEntity.notFound().build();
            }

            if (oldPrice.minorAmounts().get(subscription.currency()) >=
                newPrice.minorAmounts().get(subscription.currency())
            ) {
                return ResponseEntity.badRequest().build();
            }

            Subscription stripeSubscription = Subscription.retrieve(subscriptionId);
            String subscriptionItemId = stripeSubscription.getItems().getData().get(0).getId();

            InvoiceUpcomingParams previewParams = InvoiceUpcomingParams.builder()
                .setSubscription(subscriptionId)
                .addSubscriptionItem(
                    InvoiceUpcomingParams.SubscriptionItem.builder()
                        .setId(subscriptionItemId)
                        .setPrice(newPriceId)
                        .build()
                )
                .setSubscriptionProrationBehavior(InvoiceUpcomingParams.SubscriptionProrationBehavior.CREATE_PRORATIONS)
                .setSubscriptionProrationDate(Instant.now().getEpochSecond())
                .build();

            Invoice previewInvoice = Invoice.upcoming(previewParams);
            Long invoiceMinorAmount = previewInvoice.getLines().getData().stream()
                .map(InvoiceLineItem::getAmount)
                .limit(2)
                .reduce(Long::sum)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Invoice preview unavailable for subscription %s".formatted(subscriptionId))
                );

            UpgradePreviewResponse response = new UpgradePreviewResponse(
                invoiceMinorAmount,
                newPrice.minorAmounts().get(subscription.currency()),
                oldPrice.minorAmounts().get(subscription.currency()),
                subscription.currency()
            );

            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            LOGGER.log(Level.SEVERE, "stripe error during preview: " + subscriptionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<UpgradeConfirmationResponse> updateSubscriptionSpecification(
        String subscriptionId,
        UpdateSpecificationRequest specificationRequest
    ) {
        ContentSubscription subscription = subscriptionRepository.selectSubscription(subscriptionId)
            .orElseThrow(() -> new IllegalStateException(String.format("Cannot find subscription %s", subscriptionId)));
        String oldPriceId = subscription.priceId();
        ContentPrice oldPrice = priceRepository.selectPrice(oldPriceId)
            .orElseThrow(() -> new IllegalStateException(String.format("Cannot find price %s", oldPriceId)));
        String newPriceId = planRepository.selectPriceIdFromSpecId(specificationRequest.specificationId())
            .orElseThrow(() -> new IllegalStateException(String.format("Cannot find a plan with specification %s and currency", specificationRequest.specificationId())));
        ContentPrice newPrice = priceRepository.selectPrice(newPriceId)
            .orElseThrow(() -> new IllegalStateException(String.format("Cannot find price %s", newPriceId)));

        if (!oldPrice.minorAmounts().containsKey(subscription.currency()) ||
            !newPrice.minorAmounts().containsKey(subscription.currency())
        ) {
            return ResponseEntity.notFound().build();
        }

        if (oldPrice.minorAmounts().get(subscription.currency()) >=
            newPrice.minorAmounts().get(subscription.currency())
        ) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Subscription stripeSubscription = Subscription.retrieve(subscriptionId);
            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .addItem(
                    SubscriptionUpdateParams.Item.builder()
                        .setId(stripeSubscription.getItems().getData().getFirst().getId())
                        .setPrice(newPriceId)
                        .build()
                )
                .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS)
                .build();
            stripeSubscription.update(params);

            Invoice invoice = Invoice.create(
                InvoiceCreateParams.builder()
                    .setCustomer(stripeSubscription.getCustomer())
                    .setSubscription(stripeSubscription.getId())
                    .setAutoAdvance(true)
                    .build()
            );
            invoice.pay();

            UpgradeConfirmationResponse response = new UpgradeConfirmationResponse(
                invoice.getAmountDue(),
                newPrice.minorAmounts().get(subscription.currency()),
                subscription.currency(),
                invoice.getId()
            );
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            throw new RuntimeException("rip subscription update: " + subscriptionId, e);
        }
    }

    @Override
    public ResponseEntity<Void> updateSubscriptionTitle(String subscriptionId, UpdateTitleRequest title) {
        serverExecutionContextRepository.updateTitle(subscriptionId, title.title());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> updateSubscriptionAddress(String subscriptionId, UpdateAddressRequest address) {
        if (!address.address().endsWith(applicationConfiguration.getCloudDomain())) {
            return ResponseEntity.badRequest().build();
        }

        String subdomain = address.address().substring(0, address.address().length() - (applicationConfiguration.getCloudDomain().length() + 1));
        if (!isValidSubdomain(subdomain)) {
            return ResponseEntity.badRequest().build();
        }

        if (subscriptionRepository.updateSubdomainIfAvailable(subdomain, subscriptionId) <= 0) {
            return ResponseEntity.status(409).build();
        }

        jobScheduler.scheduleSubdomainUpdate(subscriptionId);
        return ResponseEntity.accepted().build();
    }

    private boolean isValidSubdomain(String subdomain) {
        return subdomain != null &&
            subdomain.length() >= 3 &&
            subdomain.matches("^[a-z0-9]([a-z0-9-]{0,56}[a-z0-9])?$");
    }

    private ResponseEntity<Void> updateCancelAtPeriodEnd(
        String subscriptionId,
        Boolean cancel
    ) {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .setCancelAtPeriodEnd(cancel)
                .build();
            subscription.update(params);

            return ResponseEntity.ok().build();
        } catch (StripeException e) {
            LOGGER.log(Level.SEVERE, "Stripe API error during subscription update", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }

}

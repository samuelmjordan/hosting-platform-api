package com.mc_host.api.service.stripe;

import com.mc_host.api.controller.api.SubscriptionController;
import com.mc_host.api.model.plan.ContentPrice;
import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.Mode;
import com.mc_host.api.model.provisioning.Status;
import com.mc_host.api.model.server.ProvisioningStatus;
import com.mc_host.api.model.server.response.ProvisioningStatusResponse;
import com.mc_host.api.model.stripe.request.UpdateSpecificationRequest;
import com.mc_host.api.model.subscription.ContentSubscription;
import com.mc_host.api.repository.PlanRepository;
import com.mc_host.api.repository.PriceRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionUpdateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class SubscriptionService implements SubscriptionController {
    private static final Logger LOGGER = Logger.getLogger(SubscriptionService.class.getName());

    private final ServerExecutionContextRepository serverExecutionContextRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PriceRepository priceRepository;
    private final PlanRepository planRepository;


    @Override
    public ResponseEntity<Void> cancelSubscription(String subscriptionId) {
        return updateCancelAtPeriodEnd(subscriptionId, true);
    }

    @Override
    public ResponseEntity<Void> uncancelSubscription(String subscriptionId) {
        return updateCancelAtPeriodEnd(subscriptionId, false);
    }

    @Override
    public ResponseEntity<Void> updateSubscriptionSpecification(
        String subscriptionId,
        UpdateSpecificationRequest specificationRequest
    ) {
        String oldPriceId = subscriptionRepository.selectSubscription(subscriptionId)
            .map(ContentSubscription::priceId)
            .orElseThrow(() -> new IllegalStateException(String.format("Cannot find subscription %s", subscriptionId)));
        ContentPrice oldPrice =  priceRepository.selectPrice(oldPriceId)
            .orElseThrow(() -> new IllegalStateException(String.format("Cannot find price %s", oldPriceId)));
        String newPriceId = planRepository.selectPriceId(specificationRequest.specificationId(), oldPrice.currency())
            .orElseThrow(() -> new IllegalStateException(String.format("Cannot find a plan with specification %s and currency %s", specificationRequest.specificationId(), oldPrice.currency())));
        ContentPrice newPrice =  priceRepository.selectPrice(newPriceId)
            .orElseThrow(() -> new IllegalStateException(String.format("Cannot find price %s", newPriceId)));

        if (oldPrice.minorAmount() >= newPrice.minorAmount()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .addItem(
                    SubscriptionUpdateParams.Item.builder()
                        .setId(subscription.getItems().getData().get(0).getId())
                        .setPrice(newPriceId)
                        .build()
                )
                .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS)
                .build();
                
            subscription.update(params);

            return ResponseEntity.ok().build();
        } catch (StripeException e) {
            throw new RuntimeException("rip subscription update: " + subscriptionId, e);
        }
    }

    @Override
    public ResponseEntity<ProvisioningStatusResponse> getProvisioningStatus(
        String subscriptionId
    ) {
        Context context = serverExecutionContextRepository.selectSubscription(subscriptionId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatusCode.valueOf(404), "Couldn't fetch subscription context: " + subscriptionId));

        Optional<ProvisioningStatus> status = getStatusFromContext(context, subscriptionId);
        if (status.isEmpty()) {
            LOGGER.log(Level.SEVERE, String.format("Failed to get server status: %s", subscriptionId));
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok(new ProvisioningStatusResponse(subscriptionId, status.get()));
    }

    private Optional<ProvisioningStatus> getStatusFromContext(
        Context context,
        String subscriptionId
    ) {
        if (context.isIllegalState()) {
            LOGGER.log(Level.SEVERE, String.format("Server is in an illegal state: %s", subscriptionId));
            return Optional.of(ProvisioningStatus.FAILED);
        }
        if (context.isCreated()) {
            return Optional.of(ProvisioningStatus.READY);
        }
        if (context.getMode().equals(Mode.CREATE)) {
            return Optional.of(ProvisioningStatus.PROVISIONING);
        }
        if (context.getMode().isMigrate()) {
            return Optional.of(ProvisioningStatus.MIGRATING);
        }
        if (context.isDestroyed()) {
            return Optional.of(ProvisioningStatus.INACTIVE);
        }
        if (context.getMode().equals(Mode.DESTROY)) {
            return Optional.of(ProvisioningStatus.DESTROYING);
        }
        if (context.getStatus().equals(Status.FAILED)) {
            return Optional.of(ProvisioningStatus.FAILED);
        }
        return Optional.empty();
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
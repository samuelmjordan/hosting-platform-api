package com.mc_host.api.service.stripe;

import com.mc_host.api.controller.api.SubscriptionController;
import com.mc_host.api.model.plan.ContentPrice;
import com.mc_host.api.model.stripe.request.UpdateSpecificationRequest;
import com.mc_host.api.model.subscription.ContentSubscription;
import com.mc_host.api.repository.PlanRepository;
import com.mc_host.api.repository.PriceRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.repository.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionUpdateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class SubscriptionService implements SubscriptionController {
    private static final Logger LOGGER = Logger.getLogger(SubscriptionService.class.getName());

    private final UserRepository userRepository;
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
        ContentSubscription subscription,
        UpdateSpecificationRequest specificationRequest
    ) {
        String subscriptionId = subscription.subscriptionId();
        String oldPriceId = subscription.priceId();
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
            Subscription stripeSubscription = Subscription.retrieve(subscriptionId);
            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .addItem(
                    SubscriptionUpdateParams.Item.builder()
                        .setId(stripeSubscription.getItems().getData().get(0).getId())
                        .setPrice(newPriceId)
                        .build()
                )
                .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS)
                .build();
                
            stripeSubscription.update(params);

            return ResponseEntity.ok().build();
        } catch (StripeException e) {
            throw new RuntimeException("rip subscription update: " + subscriptionId, e);
        }
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
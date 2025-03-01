package com.mc_host.api.service.stripe;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.AcceptedCurrency;
import com.mc_host.api.model.cache.CacheNamespace;
import com.mc_host.api.model.entity.ContentPrice;
import com.mc_host.api.model.entity.PricePair;
import com.mc_host.api.model.specification.SpecificationType;
import com.mc_host.api.persistence.PriceRepository;
import com.mc_host.api.util.CacheService;
import com.mc_host.api.util.Task;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.param.PriceListParams;

@Service
public class StripePriceService {
    private static final Logger LOGGER = Logger.getLogger(StripeEventProcessor.class.getName());

    private final CacheService cacheService;
    private final PriceRepository priceRepository;

    public StripePriceService(
        CacheService cacheService,
        PriceRepository priceRepository
    ) {
        this.cacheService = cacheService;
        this.priceRepository = priceRepository;
    }

    public void syncPriceData(String productId) {
        try {
            PriceListParams priceListParams = PriceListParams.builder()
                .setProduct(productId)
                .build();
            List<ContentPrice> stripePrices = Price.list(priceListParams).getData().stream()
                .map(price -> stripePriceToEntity(price, productId))
                .toList();
            List<ContentPrice> dbPrices = priceRepository.selectPricesByProductId(productId);
    
            List<ContentPrice> pricesToDelete = dbPrices.stream()
                .filter(dbPrice -> stripePrices.stream().noneMatch(dbPrice::isAlike))
                .toList();
            List<ContentPrice> pricesToCreate = stripePrices.stream()
                .filter(stripePrice -> dbPrices.stream().noneMatch(stripePrice::isAlike))
                .toList();
            List<PricePair> pricesToUpdate = dbPrices.stream()
                .flatMap(dbPrice -> stripePrices.stream()
                    .filter(dbPrice::isAlike)
                    .map(stripeSubscription -> new PricePair(dbPrice, stripeSubscription)))
                .toList();

            List<CompletableFuture<Void>> deleteTasks = pricesToDelete.stream()
                .map(price -> Task.alwaysAttempt(
                    "Delete price " + price.priceId(),
                    () -> priceRepository.deleteProductPrice(price.priceId(), productId)
                )).toList();

            List<CompletableFuture<Void>> createTasks = pricesToCreate.stream()
                .map(price -> Task.alwaysAttempt(
                    "Create price " + price.priceId(),
                    () -> priceRepository.insertPrice(price)
                )).toList();

            List<CompletableFuture<Void>> updateTasks = pricesToUpdate.stream()
                .map(pricePair -> Task.alwaysAttempt(
                    "Update price " + pricePair.getOldPrice().priceId(),
                    () -> priceRepository.insertPrice(pricePair.getNewPrice())
                )).toList();

            cacheService.evict(CacheNamespace.SPECIFICATION_PLANS, SpecificationType.fromProductId(productId).name());

            List<CompletableFuture<Void>> allTasks = new ArrayList<>();
            allTasks.addAll(deleteTasks);
            allTasks.addAll(createTasks);
            allTasks.addAll(updateTasks);
            Task.awaitCompletion(allTasks);
            
            LOGGER.log(Level.INFO, "Executed price db sync for product: " + productId);
        } catch (StripeException e) {
            LOGGER.log(Level.SEVERE, "Failed to sync price data for product: " + productId, e);
            throw new RuntimeException("Failed to sync subscription data", e);
        }
    }

    private ContentPrice stripePriceToEntity(Price price, String productId) {
        return new ContentPrice(
            price.getId(), 
            productId, 
            price.getActive(),
            AcceptedCurrency.fromCode(price.getCurrency()),
            price.getUnitAmount()
        );
    }
 
}

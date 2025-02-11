package com.mc_host.api.service;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.mc_host.api.controller.DataFetchingResource;
import com.mc_host.api.model.Currency;
import com.mc_host.api.model.entity.PriceEntity;
import com.mc_host.api.persistence.PricePersistenceService;
import com.mc_host.api.persistence.SubscriptionPersistenceService;

@Service
public class DataFetchingService implements DataFetchingResource  {
    private static final Logger LOGGER = Logger.getLogger(DataFetchingService.class.getName());

    private final CachingService cachingService;
    private final PricePersistenceService pricePersistenceService;
    private final SubscriptionPersistenceService subscriptionPersistenceService;

    public DataFetchingService(
        CachingService cachingService,
        PricePersistenceService pricePersistenceService,
        SubscriptionPersistenceService subscriptionPersistenceService
    ) {
        this.cachingService = cachingService;
        this.pricePersistenceService = pricePersistenceService;
        this.subscriptionPersistenceService = subscriptionPersistenceService;
    }

    @Override
    public ResponseEntity<List<PriceEntity>> getProductPrices(String productId) {
        try {
            LOGGER.log(Level.INFO, String.format("Fetching prices for product %s", productId));
            List<PriceEntity> prices = pricePersistenceService.selectPricesByProductId(productId);
            if (prices.isEmpty()) {
                throw new RuntimeException(String.format("ProductId %s had no prices.  Is this the correct Id?", productId));
            }
            return ResponseEntity.ok(prices);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, String.format("Failed to fetch prices for product %s: %s", productId, e.getMessage()), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<Currency> getCurrency(String userId) {
        LOGGER.log(Level.INFO, String.format("Fetching currency for user %s", userId));
        Currency currency = subscriptionPersistenceService.selectUserCurrency(userId)
            .orElse(Currency.XXX);
        return ResponseEntity.ok(currency);
    }
    
}

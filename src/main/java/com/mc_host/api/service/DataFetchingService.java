package com.mc_host.api.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mc_host.api.controller.DataFetchingResource;
import com.mc_host.api.model.Currency;
import com.mc_host.api.model.entity.PriceEntity;
import com.mc_host.api.persistence.PricePersistenceService;
import com.mc_host.api.persistence.UserPersistenceService;

@Service
public class DataFetchingService implements DataFetchingResource  {
    private static final Logger LOGGER = Logger.getLogger(DataFetchingService.class.getName());

    private final CachingService cachingService;
    private final PricePersistenceService pricePersistenceService;
    private final UserPersistenceService userPersistenceService;

    public DataFetchingService(
        CachingService cachingService,
        PricePersistenceService pricePersistenceService,
        UserPersistenceService userPersistenceService
    ) {
        this.cachingService = cachingService;
        this.pricePersistenceService = pricePersistenceService;
        this.userPersistenceService = userPersistenceService;
    }

    @Override
    public ResponseEntity<List<PriceEntity>> getProductPrices(String productId) {
        LOGGER.log(Level.INFO, String.format("Fetching prices for product %s", productId));
    
        try {
            String cacheKey = "product-prices::" + productId;
            Optional<List<PriceEntity>> cachedPrices = cachingService.retrieveCache(
                cacheKey,
                new TypeReference<List<PriceEntity>>() {}
            );
            if (cachedPrices.isPresent() && !cachedPrices.get().isEmpty()) {
                return ResponseEntity.ok(cachedPrices.get());
            }
    
            List<PriceEntity> prices = pricePersistenceService.selectPricesByProductId(productId);
            
            if (prices.isEmpty()) {
                LOGGER.log(Level.WARNING, String.format("ProductId %s had no prices. Is this the correct Id?", productId));
                throw new RuntimeException(String.format("ProductId %s had no prices. Is this the correct Id?", productId));
            }
            cachingService.cache(cacheKey, prices);
            
            return ResponseEntity.ok(prices);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, String.format("Failed to fetch prices for product %s: %s", productId, e.getMessage()), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<Currency> getCurrency(String userId) {
        LOGGER.log(Level.INFO, String.format("Fetching currency for user %s", userId));

        String cacheKey = "user-currency::" + userId;
        Optional<Currency> cachedCurrency = cachingService.retrieveCache(cacheKey, Currency.class);
        if (cachedCurrency.isPresent()) {
            return ResponseEntity.ok(cachedCurrency.get());
        }

        Optional<Currency> currency = userPersistenceService.selectUserCurrency(userId);
        if (currency.isPresent()) {
            cachingService.cache(cacheKey, currency.get());
            return ResponseEntity.ok(currency.get());
        }

        cachingService.cache(cacheKey, Currency.XXX, Duration.ofSeconds(60));
        return ResponseEntity.ok(Currency.XXX);
    }
    
}

package com.mc_host.api.service;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.mc_host.api.controller.DataFetchingResource;
import com.mc_host.api.model.entity.PriceEntity;
import com.mc_host.api.persistence.PricePersistenceService;

@Service
public class DataFetchingService implements DataFetchingResource  {
    private static final Logger LOGGER = Logger.getLogger(StripeService.class.getName());

    private final PricePersistenceService pricePersistenceService;
    private final StringRedisTemplate redisTemplate;

    public DataFetchingService(
        PricePersistenceService pricePersistenceService,
        StringRedisTemplate redisTemplate
    ) {
        this.pricePersistenceService = pricePersistenceService;
        this.redisTemplate =  redisTemplate;
    }

    @Override
    public ResponseEntity<List<PriceEntity>> getProductPrices(String productId) {
        try {
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
    
}

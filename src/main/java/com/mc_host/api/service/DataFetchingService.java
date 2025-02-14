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
import com.mc_host.api.model.CacheNamespace;
import com.mc_host.api.model.Currency;
import com.mc_host.api.model.Plan;
import com.mc_host.api.model.specification.SpecificationType;
import com.mc_host.api.persistence.PlanPersistenceService;
import com.mc_host.api.persistence.UserPersistenceService;

@Service
public class DataFetchingService implements DataFetchingResource  {
    private static final Logger LOGGER = Logger.getLogger(DataFetchingService.class.getName());

    private final CachingService cachingService;
    private final PlanPersistenceService planPersistenceService;
    private final UserPersistenceService userPersistenceService;

    public DataFetchingService(
        CachingService cachingService,
        PlanPersistenceService planPersistenceService,
        UserPersistenceService userPersistenceService
    ) {
        this.cachingService = cachingService;
        this.planPersistenceService = planPersistenceService;
        this.userPersistenceService = userPersistenceService;
    }

    @Override
    public ResponseEntity<Currency> getUserCurrency(String userId) {
        return ResponseEntity.ok(getUserCurrencyInner(userId));
    }

    public Currency getUserCurrencyInner(String userId) {
        LOGGER.log(Level.INFO, String.format("Fetching currency for clerkId %s", userId));

        CacheNamespace cacheNamespace = CacheNamespace.USER_CURRENCY;
        Optional<Currency> cachedCurrency = cachingService.retrieve(cacheNamespace, userId, Currency.class);
        if (cachedCurrency.isPresent()) {
            return cachedCurrency.get();
        }

        Optional<Currency> currency = userPersistenceService.selectUserCurrency(userId);
        if (currency.isPresent()) {
            cachingService.set(cacheNamespace, userId, currency.get());
            return cachedCurrency.get();
        }

        cachingService.set(cacheNamespace, userId, Currency.XXX, Duration.ofSeconds(60));
        return Currency.XXX;
    }

    @Override
    public ResponseEntity<List<Plan>> getPlansForSpecType(SpecificationType specType) {
        LOGGER.log(Level.INFO, String.format(String.format("Fetching plans for specType %s", specType)));
        if (specType == null) {
            return ResponseEntity.badRequest().build();
        }
    
        try {
            CacheNamespace cacheNamespace = CacheNamespace.SPECIFICATION_PLANS;
            Optional<List<Plan>> cachedPlans = cachingService.retrieve(
                cacheNamespace, specType.name(), new TypeReference<List<Plan>>() {}
            );
            if (cachedPlans.isPresent() && !cachedPlans.get().isEmpty()) {
                return ResponseEntity.ok(cachedPlans.get());
            }
    
            List<Plan> plans;
            switch(specType)  {
                case SpecificationType.JAVA_SERVER:
                    plans = planPersistenceService.selectJavaServerPlans();
                    break;
                default:
                    throw new IllegalStateException(String.format("specType %s is unhandled", specType));
            }
            
            if (plans.isEmpty()) {
                LOGGER.log(Level.WARNING, String.format("specType %s had no plans. Is this the correct Id?", specType));
                throw new RuntimeException(String.format("specType %s had no plans. Is this the correct Id?", specType));
            }
            cachingService.set(cacheNamespace, specType.name(), plans);
            
            return ResponseEntity.ok(plans);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, String.format("Failed to fetch plans for specType %s", specType), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
}

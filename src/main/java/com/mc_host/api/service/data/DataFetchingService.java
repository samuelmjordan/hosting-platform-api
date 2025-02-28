package com.mc_host.api.service.data;

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
import com.mc_host.api.model.AcceptedCurrency;
import com.mc_host.api.model.Plan;
import com.mc_host.api.model.specification.SpecificationType;
import com.mc_host.api.persistence.PlanRepository;
import com.mc_host.api.persistence.UserRepository;
import com.mc_host.api.util.CacheService;

@Service
public class DataFetchingService implements DataFetchingResource  {
    private static final Logger LOGGER = Logger.getLogger(DataFetchingService.class.getName());

    private final CacheService cacheService;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;

    public DataFetchingService(
        CacheService cacheService,
        PlanRepository planRepository,
        UserRepository userRepository
    ) {
        this.cacheService = cacheService;
        this.planRepository = planRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ResponseEntity<AcceptedCurrency> getUserCurrency(String userId) {
        return ResponseEntity.ok(getUserCurrencyInner(userId));
    }

    public AcceptedCurrency getUserCurrencyInner(String userId) {
        LOGGER.log(Level.INFO, String.format("Fetching currency for clerkId %s", userId));

        CacheNamespace cacheNamespace = CacheNamespace.USER_CURRENCY;
        Optional<AcceptedCurrency> cachedCurrency = cacheService.retrieve(cacheNamespace, userId, AcceptedCurrency.class);
        if (cachedCurrency.isPresent()) {
            return cachedCurrency.get();
        }

        Optional<AcceptedCurrency> currency = userRepository.selectUserCurrency(userId);
        if (currency.isPresent()) {
            cacheService.set(cacheNamespace, userId, currency.get());
            return currency.get();
        }

        cacheService.set(cacheNamespace, userId, AcceptedCurrency.XXX, Duration.ofSeconds(60));
        return AcceptedCurrency.XXX;
    }

    @Override
    public ResponseEntity<List<Plan>> getPlansForSpecType(SpecificationType specType) {
        LOGGER.log(Level.INFO, String.format(String.format("Fetching plans for specType %s", specType)));
        if (specType == null) {
            return ResponseEntity.badRequest().build();
        }
    
        try {
            CacheNamespace cacheNamespace = CacheNamespace.SPECIFICATION_PLANS;
            Optional<List<Plan>> cachedPlans = cacheService.retrieve(
                cacheNamespace, specType.name(), new TypeReference<List<Plan>>() {}
            );
            if (cachedPlans.isPresent() && !cachedPlans.get().isEmpty()) {
                return ResponseEntity.ok(cachedPlans.get());
            }
    
            List<Plan> plans;
            switch(specType)  {
                case SpecificationType.GAME_SERVER:
                    plans = planRepository.selectJavaServerPlans();
                    break;
                default:
                    throw new IllegalStateException(String.format("specType %s is unhandled", specType));
            }
            
            if (plans.isEmpty()) {
                LOGGER.log(Level.WARNING, String.format("specType %s had no plans. Is this the correct Id?", specType));
                throw new RuntimeException(String.format("specType %s had no plans. Is this the correct Id?", specType));
            }
            cacheService.set(cacheNamespace, specType.name(), plans);
            
            return ResponseEntity.ok(plans);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, String.format("Failed to fetch plans for specType %s", specType), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
}

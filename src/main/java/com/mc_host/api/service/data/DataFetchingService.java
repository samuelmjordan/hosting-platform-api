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
import com.mc_host.api.model.AcceptedCurrency;
import com.mc_host.api.model.Plan;
import com.mc_host.api.model.cache.CacheNamespace;
import com.mc_host.api.model.entity.ContentSubscription;
import com.mc_host.api.model.game_server.GameServer;
import com.mc_host.api.model.specification.SpecificationType;
import com.mc_host.api.repository.PlanRepository;
import com.mc_host.api.repository.UserRepository;
import com.mc_host.api.util.Cache;

@Service
public class DataFetchingService implements DataFetchingResource  {
    private static final Logger LOGGER = Logger.getLogger(DataFetchingService.class.getName());

    private final Cache cacheService;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;

    public DataFetchingService(
        Cache cacheService,
        PlanRepository planRepository,
        UserRepository userRepository
    ) {
        this.cacheService = cacheService;
        this.planRepository = planRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ResponseEntity<AcceptedCurrency> getUserCurrency(String userId) {
        LOGGER.log(Level.INFO, String.format("Fetching currency for clerkId %s", userId));

        CacheNamespace cacheNamespace = CacheNamespace.USER_CURRENCY;
        Optional<AcceptedCurrency> cachedCurrency = cacheService.retrieve(cacheNamespace, userId, AcceptedCurrency.class);
        if (cachedCurrency.isPresent()) {
            return ResponseEntity.ok(cachedCurrency.get());
        }

        Optional<AcceptedCurrency> currency = userRepository.selectUserCurrency(userId);
        if (currency.isPresent()) {
            cacheService.set(cacheNamespace, userId, currency.get());
            return ResponseEntity.ok(currency.get());
        }

        AcceptedCurrency defaulAcceptedCurrency = AcceptedCurrency.XXX;
        cacheService.set(cacheNamespace, userId, defaulAcceptedCurrency, Duration.ofHours(2));
        return ResponseEntity.ok(defaulAcceptedCurrency);
    }

    @Override
    public ResponseEntity<List<ContentSubscription>> getUserSubscriptions(String userId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUserSubscriptions'");
    }

    @Override
    public ResponseEntity<List<GameServer>> getUserServers(String userId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUserServers'");
    }

    @Override
    public ResponseEntity<List<Plan>> getUserPlans(String userId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUserPlans'");
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

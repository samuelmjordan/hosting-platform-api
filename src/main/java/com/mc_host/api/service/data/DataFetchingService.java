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
import com.mc_host.api.model.MarketingRegion;
import com.mc_host.api.model.Plan;
import com.mc_host.api.model.cache.CacheNamespace;
import com.mc_host.api.model.entity.ContentPrice;
import com.mc_host.api.model.entity.ContentSubscription;
import com.mc_host.api.model.entity.SubscriptionUserMetadata;
import com.mc_host.api.model.game_server.DnsCNameRecord;
import com.mc_host.api.model.game_server.GameServer;
import com.mc_host.api.model.response.ServerSubscriptionResponse;
import com.mc_host.api.model.specification.JavaServerSpecification;
import com.mc_host.api.model.specification.Specification;
import com.mc_host.api.model.specification.SpecificationType;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.GameServerSpecRepository;
import com.mc_host.api.repository.PlanRepository;
import com.mc_host.api.repository.PriceRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.repository.UserRepository;
import com.mc_host.api.util.Cache;

@Service
public class DataFetchingService implements DataFetchingResource  {
    private static final Logger LOGGER = Logger.getLogger(DataFetchingService.class.getName());

    private final Cache cacheService;
    private final PriceRepository priceRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final GameServerRepository gameServerRepository;
    private final GameServerSpecRepository gameServerSpecRepository;

    public DataFetchingService(
        Cache cacheService,
        PriceRepository priceRepository,
        PlanRepository planRepository,
        UserRepository userRepository,
        SubscriptionRepository subscriptionRepository,
        GameServerRepository gameServerRepository,
        GameServerSpecRepository gameServerSpecRepository
    ) {
        this.cacheService = cacheService;
        this.priceRepository = priceRepository;
        this.planRepository = planRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.gameServerRepository = gameServerRepository;
        this.gameServerSpecRepository = gameServerSpecRepository;
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

        AcceptedCurrency defaulAcceptedCurrency = AcceptedCurrency.XXX;
        cacheService.set(cacheNamespace, userId, defaulAcceptedCurrency, Duration.ofHours(2));
        return defaulAcceptedCurrency;
    }

    @Override
    public ResponseEntity<List<ServerSubscriptionResponse>> getUserServerSubscriptions(String userId) {
        LOGGER.log(Level.INFO, String.format("Fetching server subscriptions for clerkId %s", userId));

        Optional<String> customerId = getUserCustomerId(userId);
        if (!customerId.isPresent()) {
            return ResponseEntity.ok(List.of());
        }
        
        List<ServerSubscriptionResponse> serverSubscriptions = subscriptionRepository.selectSubscriptionsByCustomerId(customerId.get()).stream()
            .map(this::getServerSubscriptionResponse)
            .toList();
        return ResponseEntity.ok(serverSubscriptions);
    }

    public Optional<String> getUserCustomerId(String userId) {
        CacheNamespace cacheNamespace = CacheNamespace.USER_CUSTOMER_ID;
        Optional<Optional<String>> cachedCustomerId = cacheService.retrieve(cacheNamespace, userId, new TypeReference<Optional<String>>() {});
        if (cachedCustomerId.isPresent()) {
            return cachedCustomerId.get();
        }

        Optional<String> customerId = userRepository.selectCustomerIdByClerkId(userId);
        cacheService.set(cacheNamespace, userId, customerId, Duration.ofHours(2));
        return customerId;
    }

    private ServerSubscriptionResponse getServerSubscriptionResponse(ContentSubscription subscription) {
        ContentPrice price = priceRepository.selectPrice(subscription.priceId()).orElseThrow(
            () -> new IllegalStateException("Couldnt find price for price " + subscription.priceId()));
        String specificationId = planRepository.selectSpecificationId(subscription.priceId()).orElseThrow(
            () -> new IllegalStateException("Couldnt find specification for price " + subscription.priceId()));
        JavaServerSpecification gameSeverSpecification = gameServerSpecRepository.selectSpecification(specificationId).orElseThrow(
            () -> new IllegalStateException("Couldnt find specification for price " + subscription.priceId()));;

        String serverTitle = subscriptionRepository.selectSubscriptionUserMetadataBySubscriptionId(subscription.subscriptionId()).map(SubscriptionUserMetadata::title).orElse(null);

        String dnsCNameRecordName;
        Optional<GameServer> gameServer = gameServerRepository.selectGameServerFromSubscription(subscription.subscriptionId());
        if (!gameServer.isPresent()) {
            dnsCNameRecordName = null;
        } else {
            dnsCNameRecordName = gameServerRepository.selectDnsCNameRecord(gameServer.get().serverId()).map(DnsCNameRecord::recordName).orElse(null);
        }

        return new ServerSubscriptionResponse(
            subscription.subscriptionId(),
            serverTitle,
            gameSeverSpecification.title(),
            gameSeverSpecification.ram_gb(),
            gameSeverSpecification.vcpu(),
            MarketingRegion.valueOf(subscription.metadata().get("REGION")),
            dnsCNameRecordName,
            subscription.status(),
            subscription.currentPeriodEnd(),
            subscription.currentPeriodStart(),
            subscription.cancelAtPeriodEnd(),
            price.currency(),
            price.minorAmount()
        );
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

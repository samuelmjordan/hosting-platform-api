package com.mc_host.api.service.product;

import java.util.List;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.entity.SubscriptionEntity;
import com.mc_host.api.model.specification.SpecificationType;
import com.mc_host.api.persistence.PricePersistenceService;

@Service
public class ProductServiceSupplier {
    private final PricePersistenceService pricePersistenceService;

    private final JavaServerService javaServerService;
    private final BedrockServerService bedrockServerService;

    ProductServiceSupplier(
        PricePersistenceService pricePersistenceService,
        JavaServerService javaServerService,
        BedrockServerService bedrockServerService
    ) {
        this.pricePersistenceService = pricePersistenceService;
        this.javaServerService =  javaServerService;
        this.bedrockServerService = bedrockServerService;
    }

    public void supplyAndHandle(SubscriptionEntity subscription) {
        this.supply(subscription).handle(subscription);
    }

    public ProductService supply(SubscriptionEntity subscription) {
        SpecificationType product = SpecificationType.fromProductId(pricePersistenceService.selectProductId(subscription.priceId())
            .orElseThrow(() -> new IllegalArgumentException("Couldnt fetch product for priceId " + subscription.priceId())));

        List<ProductService> productServices = List.of(
            javaServerService,
            bedrockServerService
        );

        return productServices.stream()
            .filter(service -> service.isType(product))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Unhandled product type" + product));
    }
}

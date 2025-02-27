package com.mc_host.api.service.product;

import java.util.List;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.entity.ContentSubscription;
import com.mc_host.api.model.specification.SpecificationType;
import com.mc_host.api.persistence.PriceRepository;

@Service
public class ProductServiceSupplier {
    private final PriceRepository priceRepository;

    private final GameServerService javaServerService;
    private final AccountTierService bedrockServerService;

    ProductServiceSupplier(
        PriceRepository priceRepository,
        GameServerService javaServerService,
        AccountTierService bedrockServerService
    ) {
        this.priceRepository = priceRepository;
        this.javaServerService =  javaServerService;
        this.bedrockServerService = bedrockServerService;
    }

    public ProductService supply(ContentSubscription subscription) {
        SpecificationType product = SpecificationType.fromProductId(priceRepository.selectProductId(subscription.priceId())
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

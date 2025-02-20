package com.mc_host.api.model.specification;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum SpecificationType {
    GAME_SERVER("prod_RiiVxhDuwyX0qD"),
    ACCOUNT_TIER("BEDROCK_SERVER::placeholder");

    private String productId;

    private static final Map<String, SpecificationType> ID_MAP = 
        Stream.of(values()).collect(Collectors.toMap(
            SpecificationType::getProductId,
            product -> product
    ));

    SpecificationType(String productId) {
        this.productId = productId;
    }

    public static SpecificationType fromProductId(String productId) {
        SpecificationType result = ID_MAP.get(productId);
        if (result == null) {
            throw new IllegalArgumentException("no enum found for id: " + productId);
        }
        return result;
    }

    public String getProductId() {
        return productId;
    }
}

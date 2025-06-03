package com.mc_host.api.model.stripe.response;

import java.util.Map;

public record PaymentMethodResponse(
    String id,
    String type,
    String displayName,
    boolean isDefault,
    boolean isActive,
    Map<String, DisplayField> fields
) {
    public record DisplayField(
        String value,
        String label, 
        String displayType
    ) {}
}

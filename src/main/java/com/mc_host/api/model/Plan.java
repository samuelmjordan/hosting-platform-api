package com.mc_host.api.model;

import com.mc_host.api.model.entity.PriceEntity;
import com.mc_host.api.model.specification.Specification;

public record Plan(
    Specification specification,
    PriceEntity price
) {
}

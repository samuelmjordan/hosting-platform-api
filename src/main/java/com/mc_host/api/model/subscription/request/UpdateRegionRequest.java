package com.mc_host.api.model.subscription.request;

import com.mc_host.api.model.subscription.MarketingRegion;

public record UpdateRegionRequest(
    MarketingRegion region
) {
}

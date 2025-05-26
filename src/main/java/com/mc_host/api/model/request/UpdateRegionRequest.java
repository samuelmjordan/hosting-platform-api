package com.mc_host.api.model.request;

import com.mc_host.api.model.MarketingRegion;

public record UpdateRegionRequest(
    MarketingRegion region
) {
}

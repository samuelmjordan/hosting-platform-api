package com.mc_host.api.model.subscription;

import com.mc_host.api.model.resource.hetzner.HetznerRegion;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum MarketingRegion {
    WEST_EUROPE,
    EAST_EUROPE;

    public HetznerRegion getFirstHetznerRegion() {
        return Arrays.stream(HetznerRegion.values())
            .filter(hetznerRegion -> this.equals(hetznerRegion.getMarketingRegion()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(String.format("No hetzner region exists for marketing region %s", this.name())));
    }
}

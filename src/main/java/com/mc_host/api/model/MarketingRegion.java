package com.mc_host.api.model;

import com.mc_host.api.model.hetzner.HetznerRegion;

import java.util.Arrays;

import lombok.Getter;

@Getter
public enum MarketingRegion {
    WEST_EUROPE(),
    EAST_EUROPE();

    public HetznerRegion getHetznerRegion() {
        return Arrays.stream(HetznerRegion.values())
            .filter(hetznerRegion -> this.equals(hetznerRegion.getMarketingRegion()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(String.format("No hetzner region exists for marketing region %s", this.name())));
    }
}

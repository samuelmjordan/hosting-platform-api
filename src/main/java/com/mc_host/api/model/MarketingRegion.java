package com.mc_host.api.model;

import java.util.List;

import com.mc_host.api.model.hetzner.HetznerRegion;

public enum MarketingRegion {
    NORTH_AMERICA_EAST(List.of(HetznerRegion.ASH)),
    NORTH_AMERICA_WEST(List.of(HetznerRegion.HIL)),
    WEST_EUROPE(List.of(HetznerRegion.NBG1, HetznerRegion.FSN1)),
    EAST_EUROPE(List.of(HetznerRegion.HEL1)),
    ASIA(List.of());

    public final List<HetznerRegion> hetznerRegions;

    MarketingRegion(
        List<HetznerRegion> hetznerRegions
    ) {
        this.hetznerRegions = hetznerRegions;
    }
}

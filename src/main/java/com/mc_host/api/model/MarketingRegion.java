package com.mc_host.api.model;

import java.util.List;

import com.mc_host.api.model.hetzner.HetznerRegion;

import lombok.Getter;

@Getter
public enum MarketingRegion {
    WEST_EUROPE(List.of(HetznerRegion.NBG1, HetznerRegion.FSN1)),
    EAST_EUROPE(List.of(HetznerRegion.HEL1));

    public final List<HetznerRegion> hetznerRegions;

    MarketingRegion(
        List<HetznerRegion> hetznerRegions
    ) {
        this.hetznerRegions = hetznerRegions;
    }
}

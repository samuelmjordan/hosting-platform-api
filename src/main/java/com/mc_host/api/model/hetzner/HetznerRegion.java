package com.mc_host.api.model.hetzner;

import com.mc_host.api.model.Region;

import lombok.Getter;

@Getter
public enum HetznerRegion {
    NBG1(Region.WEST_EUROPE, 1);

    public Region marketingRegionMapping;
    public Integer pterodactylLocationId;

    HetznerRegion(
        Region regionMapping,
        Integer pterodactylLocationId
    ) {
        this.marketingRegionMapping = regionMapping;
        this.pterodactylLocationId = pterodactylLocationId;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}

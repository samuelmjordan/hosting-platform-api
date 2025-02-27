package com.mc_host.api.model.hetzner;

import com.mc_host.api.model.MarketingRegion;

import lombok.Getter;

@Getter
public enum HetznerRegion {
    NBG1(MarketingRegion.WEST_EUROPE, 1);

    public final MarketingRegion marketingRegionMapping;
    public final Integer pterodactylLocationId;

    HetznerRegion(
        MarketingRegion regionMapping,
        Integer pterodactylLocationId
    ) {
        this.marketingRegionMapping = regionMapping;
        this.pterodactylLocationId = pterodactylLocationId;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }

    public static HetznerRegion lookup(String string)  {
        return HetznerRegion.valueOf(string.toUpperCase());
    }
}

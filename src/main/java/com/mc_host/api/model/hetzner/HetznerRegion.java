package com.mc_host.api.model.hetzner;

import com.mc_host.api.model.MarketingRegion;

import lombok.Getter;

@Getter
public enum HetznerRegion {
    NBG1(MarketingRegion.WEST_EUROPE, 1),
    FSN1(MarketingRegion.WEST_EUROPE, 2),
    HEL1(MarketingRegion.EAST_EUROPE, 3);

    private final MarketingRegion marketingRegion;
    private final Integer pterodactylLocationId;

    HetznerRegion(
        MarketingRegion regionMapping,
        Integer pterodactylLocationId
    ) {
        this.marketingRegion = regionMapping;
        this.pterodactylLocationId = pterodactylLocationId;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }

    public static HetznerRegion lookup(String string)  {
        try {
            return HetznerRegion.valueOf(string.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Cannot lookup HetznerRegion from string '%s'", string));
        }
    }
}

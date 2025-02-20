package com.mc_host.api.model.hetzner;

public enum HetznerRegion {
    NBG1;

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}

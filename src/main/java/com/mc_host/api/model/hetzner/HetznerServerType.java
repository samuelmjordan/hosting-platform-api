package com.mc_host.api.model.hetzner;

public enum HetznerServerType {
    CAX11;

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}

package com.mc_host.api.model.hetzner;

public enum HetznerServerType {
    CAX11("7bf955c1-8072-4812-87fc-a096af2485bf");

    private final String specificationId;

    HetznerServerType(String specificationId) {
        this.specificationId = specificationId;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}

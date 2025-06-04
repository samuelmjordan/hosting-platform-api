package com.mc_host.api.model.resource.hetzner;

public enum HetznerSpec {
    CAX11("7bf955c1-8072-4812-87fc-a096af2485bf");

    private final String specificationId;

    HetznerSpec(String specificationId) {
        this.specificationId = specificationId;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}

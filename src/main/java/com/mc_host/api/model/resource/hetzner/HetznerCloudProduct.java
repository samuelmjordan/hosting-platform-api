package com.mc_host.api.model.resource.hetzner;

import lombok.Getter;

@Getter
public enum HetznerCloudProduct {
    CAX11("7bf955c1-8072-4812-87fc-a096af2485bf"),
    CAX21("14f90ac7-7c0f-4cf9-97c9-fb69edb5f823"),
    CAX31("0433bed8-6964-40ce-8d70-478be68251b2");

    private final String specificationId;

    HetznerCloudProduct(String specificationId) {
        this.specificationId = specificationId;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }

    public static HetznerCloudProduct fromSpecificationId(String specificationId) {
        for (HetznerCloudProduct spec: HetznerCloudProduct.values()) {
            if (spec.getSpecificationId().equals(specificationId)) return spec;
        }
        throw new IllegalStateException("No hetzner cloud spec matches this id: " + specificationId);
    }

    public static HetznerCloudProduct lookup(String string)  {
        try {
            return HetznerCloudProduct.valueOf(string.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("No HetznerSpec for string '%s'", string));
        }
    }
}

package com.mc_host.api.model.resource;

import java.util.UUID;

public record Node(
    String nodeId,
    boolean dedicated
) {

    public static Node newCloudNode() {
        return new Node(UUID.randomUUID().toString(), false);
    }

    public static Node newDedicatedNode() {
        return new Node(UUID.randomUUID().toString(), true);
    }
}
package com.mc_host.api.model.resource.hetzner;

import java.util.List;

import com.mc_host.api.model.resource.hetzner.HetznerServerResponse.Server;

public record HetznerServersResponse(
    List<Server> servers,
    HetznerMetadata meta
) {
}
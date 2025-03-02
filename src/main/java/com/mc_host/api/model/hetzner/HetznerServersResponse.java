package com.mc_host.api.model.hetzner;

import java.util.List;

import com.mc_host.api.model.hetzner.HetznerServerResponse.Server;

public record HetznerServersResponse(
    List<Server> servers,
    HetznerMetadata meta
) {
}
package com.mc_host.api.model.resource.hetzner;

public record HetznerPagination(
    int page,
    int perPage,
    Integer previousPage,
    Integer nextPage,
    int lastPage,
    int totalEntries
) {
}
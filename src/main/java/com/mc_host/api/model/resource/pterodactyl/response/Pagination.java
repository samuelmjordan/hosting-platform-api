package com.mc_host.api.model.resource.pterodactyl.response;

public record Pagination(
    int total,
    int count,
    int perPage,
    int currentPage,
    int totalPages
) {}

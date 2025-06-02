package com.mc_host.api.model.pterodactyl.response;

public record Pagination(
    int total,
    int count,
    int perPage,
    int currentPage,
    int totalPages
) {}

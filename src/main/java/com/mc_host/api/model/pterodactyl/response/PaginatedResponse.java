package com.mc_host.api.model.pterodactyl.response;

import java.util.List;

public record PaginatedResponse<T>(
    List<T> data, 
    Metadata meta
) {}
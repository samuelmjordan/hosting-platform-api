package com.mc_host.api.model.resource.pterodactyl.request;

import java.util.List;

public record RenameRequest(
    String directory,
    List<RenameMapping> files
) {
    
}

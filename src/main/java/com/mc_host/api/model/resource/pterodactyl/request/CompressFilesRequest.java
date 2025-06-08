package com.mc_host.api.model.resource.pterodactyl.request;

import java.util.List;

public record CompressFilesRequest(
    String root,
    List<String> files
) {

}

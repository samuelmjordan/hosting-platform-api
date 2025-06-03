package com.mc_host.api.model.resource.pterodactyl.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PterodactylCreateNodeRequest {
    private final String name;
    private final String description;
    @JsonProperty("location_id")  // <- these are crucial
    private final Integer locationId;
    @JsonProperty("public")
    private final Boolean public_;
    private final String fqdn;
    private final String scheme;
    @JsonProperty("behind_proxy")
    private final Boolean behindProxy;
    private final Integer memory;
    @JsonProperty("memory_overallocate")
    private final Integer memoryOverallocate;
    private final Integer disk;
    @JsonProperty("disk_overallocate")
    private final Integer diskOverallocate;
    @JsonProperty("upload_size")
    private final Integer uploadSize;
    @JsonProperty("daemon_sftp")
    private final Integer daemonSftp;
    @JsonProperty("daemon_listen")
    private final Integer daemonListen;  
}

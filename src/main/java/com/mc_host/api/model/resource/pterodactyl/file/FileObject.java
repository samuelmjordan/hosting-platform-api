package com.mc_host.api.model.resource.pterodactyl.file;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FileObject(
    String object,
    FileAttributes attributes
) {
    public record FileAttributes(
        String name,
        String mode,
        long size,
        @JsonProperty("is_file") boolean isFile,
        @JsonProperty("is_symlink") boolean isSymlink,
        @JsonProperty("is_editable") boolean isEditable,
        String mimetype,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("modified_at") String modifiedAt
    ) {}
}
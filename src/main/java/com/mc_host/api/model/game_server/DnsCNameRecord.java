package com.mc_host.api.model.game_server;

public record DnsCNameRecord(
    String serverId,
    String cNameRecordId,
    String zoneId,
    String zoneName,
    String recordName,
    String content
) {}

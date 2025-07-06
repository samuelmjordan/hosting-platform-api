package com.mc_host.api.model.resource.dns;

public record DnsCNameRecord(
    String subscriptionId,
    String cNameRecordId,
    String zoneId,
    String zoneName,
    String recordName,
    String content
) {}

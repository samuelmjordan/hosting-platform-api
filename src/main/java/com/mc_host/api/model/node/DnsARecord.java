package com.mc_host.api.model.node;

public record DnsARecord(
    String nodeId,
    String aRecordId,
    String zoneId,
    String zoneName,
    String recordName,
    String content
) {}
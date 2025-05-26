package com.mc_host.api.model.node;

public record DnsARecord(
    String subscriptionId,
    String aRecordId,
    String zoneId,
    String zoneName,
    String recordName,
    String content
) {}
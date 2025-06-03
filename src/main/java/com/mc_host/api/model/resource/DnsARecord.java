package com.mc_host.api.model.resource;

public record DnsARecord(
    String subscriptionId,
    String aRecordId,
    String zoneId,
    String zoneName,
    String recordName,
    String content
) {}
package com.mc_host.api.model.node;

public record DnsARecord(
    String nodeId,
    String aRecordId,
    String zoneName,
    String recordName,
    String ipv4
) {}
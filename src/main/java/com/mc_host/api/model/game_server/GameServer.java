package com.mc_host.api.model.game_server;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class GameServer {
    private final String serverId;
    private final String subscriptionId;
    private final String planId;
    private final String nodeId;

    private Long pterodactylServerId;
    private Long allocationId;
    private Integer port;
    private String cNameRecordId;
    private String zoneName;
    private String recordName;
}

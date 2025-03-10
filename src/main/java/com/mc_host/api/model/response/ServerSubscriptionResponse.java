package com.mc_host.api.model.response;

import com.mc_host.api.model.entity.ContentSubscription;
import com.mc_host.api.model.game_server.DnsCNameRecord;
import com.mc_host.api.model.game_server.GameServer;

public record ServerSubscriptionResponse(
    ContentSubscription subscription,
    GameServer gameServer,
    DnsCNameRecord dnsCNameRecord
) {
}

package com.mc_host.api.model.resource.pterodactyl.panel;

import java.util.List;

public record WebsocketEvent(
    String event,
    List<String> args
) {
    
}

package com.mc_host.api.model.entity;

public record JavaServerEntity(
    String server_id,
    String hetzner_id,
    String pterodactyl_id,
    String subscription_id,
    String plan_id
) {
    
}

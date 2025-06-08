package com.mc_host.api.model.resource.pterodactyl;

public record PterodactylServerResources(
    String current_state,
    boolean is_suspended,
    long memory_bytes,  
    long memory_limit_bytes,
    double cpu_absolute,
    long network_rx_bytes,
    long network_tx_bytes,
    long disk_bytes,
    String uptime
) {
    
}

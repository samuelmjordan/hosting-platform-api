package com.mc_host.api.model.provisioning;

public enum StepType {
    NEW,
    ALLOCATE_NODE,
    CLOUD_NODE,
    DEDICATED_NODE,
    A_RECORD,
    PTERODACTYL_NODE,
    CONFIGURE_NODE,
    PTERODACTYL_ALLOCATION,
    PTERODACTYL_SERVER,
    C_NAME_RECORD,
    TRANSFER_DATA,
    START_SERVER,
    FINALISE,
    READY
}

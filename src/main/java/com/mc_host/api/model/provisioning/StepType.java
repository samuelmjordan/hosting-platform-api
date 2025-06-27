package com.mc_host.api.model.provisioning;

public enum StepType {
    //Inactive terminal
    NEW,

    //Dedicated node prep
    TRY_ALLOCATE_DEDICATED_NODE,

    //Cloud node prep
    ALLOCATE_CLOUD_NODE,
    A_RECORD,
    PTERODACTYL_NODE,
    CONFIGURE_NODE,
    CREATE_PTERODACTYL_ALLOCATION,

    //Server prep
    ASSIGN_PTERODACTYL_ALLOCATION,
    PTERODACTYL_SERVER,
    INSTALL_SERVER,
    CREATE_SUBUSER,
    //Migration specific
    TRANSFER_DATA,
    //Server prep
    START_SERVER,

    //Domain name prep
    C_NAME_RECORD,
    SYNC_NODE_ROUTE,

    //Non-migration specific
    FINALISE,

    //Active terminal
    READY
}

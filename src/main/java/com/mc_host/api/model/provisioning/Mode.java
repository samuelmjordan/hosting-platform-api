package com.mc_host.api.model.provisioning;

import java.util.List;

public enum Mode {
    CREATE,
    DESTROY,

    MIGRATE_CREATE,
    MIGRATE_DESTROY;

    public Boolean isCreate() {
        return List.of(CREATE, MIGRATE_CREATE).contains(this);
    }

    public Boolean isDestroy() {
        return List.of(DESTROY, MIGRATE_DESTROY).contains(this);
    }

    public Boolean isMigrate() {
        return List.of(MIGRATE_CREATE, MIGRATE_DESTROY).contains(this);
    }
}

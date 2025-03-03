package com.mc_host.api.service.reconciliation;

import com.mc_host.api.model.ResourceType;

public interface ResourceReconciler {
    ResourceType getType();
    void reconcile();
}

package com.mc_host.api.service.reconciliation;

import com.mc_host.api.model.resource.ResourceType;

public interface ResourceReconciler {
    ResourceType getType();
    void reconcile();
}

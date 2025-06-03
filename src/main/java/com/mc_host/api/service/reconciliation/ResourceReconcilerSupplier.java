package com.mc_host.api.service.reconciliation;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.resource.ResourceType;

@Service
public class ResourceReconcilerSupplier {

    private final Map<ResourceType, ResourceReconciler> reconcilerMap;

    public ResourceReconcilerSupplier(
        HetznerCloudNodeReconciler hetznerNodeReconciler,
        ARecordReconciler aRecordReconciler
    ) {
        this.reconcilerMap = Map.of(
            ResourceType.HETZNER_NODE, hetznerNodeReconciler,
            ResourceType.A_RECORD, aRecordReconciler
        );
    }
    
    public ResourceReconciler supply(ResourceType resourceType) {
        return reconcilerMap.get(resourceType);
    }
}

package com.mc_host.api.model.entity.server;

public interface ProvisionableServer {
    ProvisioningState getProvisioningState();
    ProvisioningStatus getProvisioningStatus();

    void setProvisioningState(ProvisioningState state);
    void setProvisioningStatus(ProvisioningStatus status);

    default void transitionState(ProvisioningState nextState) {
        getProvisioningState().validateTransition(nextState);
        setProvisioningState(nextState);
    }
}

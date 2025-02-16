package com.mc_host.api.model.entity.server;

public interface ProvisionableServer {
    ProvisioningState getProvisioningState();
    Integer getRetryCount();

    void setProvisioningState(ProvisioningState state);
    void setRetryCount(Integer count);

    default void transitionState(ProvisioningState nextState) {
        getProvisioningState().validateTransition(nextState);
        setProvisioningState(nextState);
    }

    default void incrementRetryCount() {
        setRetryCount(getRetryCount() + 1);
    }

    default void resetIncrementCount() {
        setRetryCount(0);
    }
}

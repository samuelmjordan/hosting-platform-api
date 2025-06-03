package com.mc_host.api.model.resource;

import java.util.Set;

public enum ProvisioningState {
    NEW,
    NODE_PROVISIONED,
    NODE_CONFIGURED,
    READY;

    private Set<ProvisioningState> nextPossibleStates;

    ProvisioningState() {
        nextPossibleStates = Set.of();
    }

    static {
        NEW.nextPossibleStates = validTransitions(NODE_PROVISIONED);
        NODE_PROVISIONED.nextPossibleStates = validTransitions(NODE_CONFIGURED);
        NODE_CONFIGURED.nextPossibleStates = validTransitions(READY);
        READY.nextPossibleStates = validTransitions();
    }

    private static Set<ProvisioningState> validTransitions(ProvisioningState... states) {
        return Set.of(states);
    }

    public boolean canTransitionTo(ProvisioningState nextState) {
        return nextPossibleStates.contains(nextState);
    }

    public Set<ProvisioningState> getNextPossibleStates() {
        return nextPossibleStates;
    }

    public void validateTransition(ProvisioningState nextState) {
        if (!canTransitionTo(nextState)) {
            throw new IllegalStateException(String.format("Invalid transition from %s to %s", this, nextState));
        }
    }
}

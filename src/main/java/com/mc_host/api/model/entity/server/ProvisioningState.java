package com.mc_host.api.model.entity.server;

import java.util.Set;

public enum ProvisioningState {
    NEW,
    METAL_PROVISIONED,
    NODE_PROVISIONED,
    
    PRE_MIGRATION,
    MIGRATION_METAL_DESTROYED,
    MIGRATION_METAL_PROVISIONED,
    MIGRATION_NODE_DESTROYED,
    MIGRATION_NODE_PROVISIONED,
    READY,
    
    PRE_SHUTDOWN,
    SHUTDOWN_NODE_DESTROYED,
    SHUTDOWN_METAL_DESTROYED,
    SHUTDOWN;

    private Set<ProvisioningState> nextPossibleStates;

    ProvisioningState() {
        nextPossibleStates = Set.of();
    }

    static {
        NEW.nextPossibleStates = validTransitions(METAL_PROVISIONED);
        METAL_PROVISIONED.nextPossibleStates = validTransitions(NODE_PROVISIONED);
        NODE_PROVISIONED.nextPossibleStates = validTransitions(READY);
        PRE_MIGRATION.nextPossibleStates = validTransitions(MIGRATION_METAL_DESTROYED);
        MIGRATION_METAL_DESTROYED.nextPossibleStates = validTransitions(MIGRATION_METAL_PROVISIONED);
        MIGRATION_METAL_PROVISIONED.nextPossibleStates = validTransitions(MIGRATION_NODE_DESTROYED);
        MIGRATION_NODE_DESTROYED.nextPossibleStates = validTransitions(MIGRATION_NODE_PROVISIONED);
        MIGRATION_NODE_PROVISIONED.nextPossibleStates = validTransitions(READY);
        READY.nextPossibleStates = validTransitions(PRE_MIGRATION, PRE_SHUTDOWN);
        PRE_SHUTDOWN.nextPossibleStates = validTransitions(SHUTDOWN_NODE_DESTROYED);
        SHUTDOWN_NODE_DESTROYED.nextPossibleStates = validTransitions(SHUTDOWN_METAL_DESTROYED);
        SHUTDOWN_METAL_DESTROYED.nextPossibleStates = validTransitions(SHUTDOWN);
        SHUTDOWN.nextPossibleStates = validTransitions();
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

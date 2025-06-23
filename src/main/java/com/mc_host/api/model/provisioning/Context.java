package com.mc_host.api.model.provisioning;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;

@Getter
@With
@AllArgsConstructor
public class Context {
    private final String subscriptionId;
    private final StepType stepType;
    private final Mode mode;
    private final Status status;

    private final String title;
    private final String caption;

    private final Boolean recreate;

    // Current resources
    private final Long nodeId;
    private final String aRecordId;
    private final Long pterodactylNodeId;
    private final Long allocationId;
    private final Long pterodactylServerId;
    private final String cNameRecordId;

    // New resources (migration targets)
    private final Long newNodeId;
    private final String newARecordId;
    private final Long newPterodactylNodeId;
    private final Long newAllocationId;
    private final Long newPterodactylServerId;
    private final String newCNameRecordId;

    public static Context newIdle(
        String subscriptionId,
        String title, 
        String caption) {
        return new Context(
            subscriptionId,
            StepType.NEW,
            Mode.DESTROY,
            Status.COMPLETED,
            title,
            caption,
            false,
            null, null, null, null, null, null, // current resources
            null, null, null, null, null, null  // new resources
        );
    }

    public Context inProgress() {
        return this.withStatus(Status.IN_PROGRESS);
    }

    public Context completed() {
        return this.withStatus(Status.COMPLETED);
    }

    public Context failed() {
        return this.withStatus(Status.FAILED);
    }
    
    public Context transitionTo(StepType nextStep) {
        return this.withStepType(nextStep).inProgress();
    }

    public Boolean isCreated() {
        return 
            stepType == StepType.READY &&
            status == Status.COMPLETED &&
            mode == Mode.CREATE;
    }

    public Boolean isDestroyed() {
        return 
            stepType == StepType.NEW &&
            status == Status.COMPLETED &&
            mode == Mode.DESTROY;
    }

    public Boolean isTerminal() {
        return
            this.isCreated() ||
            this.isDestroyed();
    }

    // Resource promotion methods
    public Context promoteNewNodeId() {
        return this.withNodeId(this.newNodeId).withNewNodeId(null);
    }

    public Context promoteNewARecordId() {
        return this.withARecordId(this.newARecordId).withNewARecordId(null);
    }

    public Context promoteNewPterodactylNodeId() {
        return this.withPterodactylNodeId(this.newPterodactylNodeId).withNewPterodactylNodeId(null);
    }

    public Context promoteNewAllocationId() {
        return this.withAllocationId(this.newAllocationId).withNewAllocationId(null);
    }

    public Context promoteNewPterodactylServerId() {
        return this.withPterodactylServerId(this.newPterodactylServerId).withNewPterodactylServerId(null);
    }

    public Context promoteNewCNameRecordId() {
        return this.withCNameRecordId(this.newCNameRecordId).withNewCNameRecordId(null);
    }

    public Context promoteAllNewResources() {
        return this
            .withNodeId(this.newNodeId)
            .withARecordId(this.newARecordId)
            .withPterodactylNodeId(this.newPterodactylNodeId)
            .withAllocationId(this.newAllocationId)
            .withPterodactylServerId(this.newPterodactylServerId)
            .withCNameRecordId(this.newCNameRecordId)
            .withNewNodeId(null)
            .withNewARecordId(null)
            .withNewPterodactylNodeId(null)
            .withNewAllocationId(null)
            .withNewPterodactylServerId(null)
            .withNewCNameRecordId(null);
    }
}
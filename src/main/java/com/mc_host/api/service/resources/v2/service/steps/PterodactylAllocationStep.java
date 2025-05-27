package com.mc_host.api.service.resources.v2.service.steps;

import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;

public class PterodactylAllocationStep extends AbstractStep {

    protected PterodactylAllocationStep(
        ServerExecutionContextRepository contextRepository
    ) {
        super(contextRepository);
    }

    @Override
    public StepType getType() {
        return StepType.PTERODACTYL_ALLOCATION;
    }

    @Override
    public StepTransition create(Context context) {
        return inProgress(context, StepType.PTERODACTYL_SERVER);
    }

    @SuppressWarnings("unused")
    @Override
    public StepTransition destroy(Context context) {
        // TODO: identify is server is hosted on a dedicated or cloud node
        // For now, we assume that the server is hosted on a cloud node
        if (false) {
            return inProgress(context, StepType.DEDICATED_NODE);
        }
        return inProgress(context, StepType.PTERODACTYL_NODE);
    }

    @Override
    public StepTransition migrate(Context context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'migrate'");
    }

    @Override
    public StepTransition update(Context context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }

}

package com.mc_host.api.service.resources.v2.service.steps;

import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;

public class PterodactylNodeStep extends AbstractStep {

    protected PterodactylNodeStep(
        ServerExecutionContextRepository contextRepository
    ) {
        super(contextRepository);
    }

    @Override
    public StepType getType() {
        return StepType.PTERODACTYL_NODE;
    }

    @Override
    public StepTransition create(Context context) {
        return inProgress(context, StepType.CONFIGURE_NODE);
    }

    @Override
    public StepTransition destroy(Context context) {
        return inProgress(context, StepType.A_RECORD);
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

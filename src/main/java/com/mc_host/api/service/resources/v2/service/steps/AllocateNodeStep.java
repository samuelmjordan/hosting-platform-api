package com.mc_host.api.service.resources.v2.service.steps;

import org.springframework.stereotype.Service;

import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;

@Service
public class AllocateNodeStep extends AbstractStep {

    protected AllocateNodeStep(
        ServerExecutionContextRepository contextRepository
    ) {
        super(contextRepository);
    }

    @Override
    public StepType getType() {
        return StepType.ALLOCATE_NODE;
    }

    @SuppressWarnings("unused")
    @Override
    public StepTransition create(Context context) {
        // TODO: use dedicated node if available
        if (false) {
            return inProgress(context, StepType.DEDICATED_NODE);
        }
        return inProgress(context, StepType.CLOUD_NODE);
    }

    @Override
    public StepTransition destroy(Context context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'destroy'");
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

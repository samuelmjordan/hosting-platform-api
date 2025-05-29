package com.mc_host.api.service.resources.v2.service.steps;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.Mode;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;
import com.mc_host.api.service.resources.v2.service.TransitionService;

@Service
public class NewStep extends AbstractStep {

    protected NewStep(
        ServerExecutionContextRepository contextRepository,
        TransitionService transitionService
    ) {
        super(contextRepository, transitionService);
    }

    @Override
    public StepType getType() {
        return StepType.NEW;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        return transitionService.persistAndProgress(context, StepType.ALLOCATE_NODE);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        if (context.getMode().isMigrate()) {
            return transitionService.persistAndProgress(context.withMode(Mode.CREATE), StepType.READY);        
        }
        return transitionService.persistAndComplete(context);
    }

}

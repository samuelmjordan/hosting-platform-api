package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.Mode;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.provisioning.TransitionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

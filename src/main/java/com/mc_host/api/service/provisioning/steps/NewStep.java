package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.Mode;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NewStep extends AbstractStep {

    @Override
    public StepType getType() {
        return StepType.NEW;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        return transitionService.persistAndProgress(context, StepType.TRY_ALLOCATE_DEDICATED_NODE);
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

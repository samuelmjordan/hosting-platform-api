package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//TODO
@Service
@RequiredArgsConstructor
public class AllocateDedicatedNodeStep extends AbstractStep {

    @Override
    public StepType getType() {
        return StepType.TRY_ALLOCATE_DEDICATED_NODE;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        //Atomic statement
        //Check and claim dedicated resources
        //Return true if successful

        if (false) {
            //If dedicated resources are claimed, cloud node setup can be skipped
            return transitionService.persistAndProgress(context, StepType.PTERODACTYL_SERVER);
        }

        //Else proceed with cloud provisioning
        return transitionService.persistAndProgress(context, StepType.ALLOCATE_CLOUD_NODE);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        //Skip for cloud resources
        if (true) {
            LOGGER.warning("%s step is illegal for cloud resources. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
            return transitionService.persistAndProgress(context, StepType.NEW);
        }

        return transitionService.persistAndProgress(context, StepType.NEW);
    }

}

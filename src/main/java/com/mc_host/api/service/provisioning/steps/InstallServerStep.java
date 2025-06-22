package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.provisioning.TransitionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstallServerStep extends AbstractStep {

    protected InstallServerStep(
		ServerExecutionContextRepository contextRepository,
		TransitionService transitionService
    ) {
        super(contextRepository, transitionService);
    }

    @Override
    public StepType getType() {
        return StepType.INSTALL_SERVER;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        //TODO: proper polling logic for server install status
		try {
			Thread.sleep(20_000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		return transitionService.persistAndProgress(context, StepType.CREATE_SUBUSER);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        throw new UnsupportedOperationException("Server Installation step cannot be destroyed directly. Try destroying the Pterodactyl Node step instead.");
    }

}

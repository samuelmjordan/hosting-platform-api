package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.panel.TransferService;
import com.mc_host.api.service.provisioning.TransitionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferDataStep extends AbstractStep {

    private final TransferService transferService;

    protected TransferDataStep(
		ServerExecutionContextRepository contextRepository,
		TransitionService transitionService,
        TransferService transferService
	) {
        super(contextRepository, transitionService);
		this.transferService = transferService;
	}

    @Override
    public StepType getType() {
        return StepType.TRANSFER_DATA;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        if (!context.getMode().isMigrate()) {
            throw new UnsupportedOperationException("Data Transfer step is migration only.");
        }
        try {
            //transferService.transferServerData(context.getPterodactylServerId(), context.getNewPterodactylServerId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        return transitionService.persistAndProgress(null, StepType.START_SERVER);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        throw new UnsupportedOperationException("Data Transfer step cannot be destroyed directly.");
    }

}

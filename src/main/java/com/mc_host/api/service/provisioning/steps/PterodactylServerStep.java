package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.plan.ServerSpecification;
import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.pterodactyl.PterodactylAllocation;
import com.mc_host.api.model.resource.pterodactyl.PterodactylServer;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.GameServerSpecRepository;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.provisioning.TransitionService;
import com.mc_host.api.service.resources.PterodactylService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PterodactylServerStep extends AbstractStep {

    private final NodeRepository nodeRepository;
    private final GameServerRepository gameServerRepository;
    private final GameServerSpecRepository gameServerSpecRepository;
    private final PterodactylService pterodactylService;

    protected PterodactylServerStep(
        ServerExecutionContextRepository contextRepository,
        GameServerRepository gameServerRepository,
        GameServerSpecRepository gameServerSpecRepository,
        TransitionService transitionService,
        NodeRepository nodeRepository,
        PterodactylService pterodactylService
    ) {
        super(contextRepository, transitionService);
        this.nodeRepository = nodeRepository;
        this.gameServerRepository = gameServerRepository;
        this.pterodactylService = pterodactylService;
        this.gameServerSpecRepository = gameServerSpecRepository;
    }

    @Override
    public StepType getType() {
        return StepType.PTERODACTYL_SERVER;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        PterodactylAllocation allocationAttributes = nodeRepository.selectPterodactylAllocation(context.getNewAllocationId())
            .orElseThrow(() -> new IllegalStateException("Pterodactyl allocation not found: " + context));
        ServerSpecification serverSpecification = gameServerSpecRepository.selectSpecification(context.getSpecificationId())
            .orElseThrow(() -> new IllegalStateException("DNS A Record not found:" + context.getNewARecordId()));
        PterodactylServer pterodactylServer = pterodactylService.createServer(context.getSubscriptionId(), allocationAttributes, serverSpecification);

        Context transitionedContext = context.withNewPterodactylServerId(pterodactylServer.pterodactylServerId());
        gameServerRepository.insertPterodactylServer(pterodactylServer);

        if (context.getMode().isMigrate()) {
            return transitionService.persistAndProgress(transitionedContext, StepType.TRANSFER_DATA);
        }
        return transitionService.persistAndProgress(transitionedContext, StepType.C_NAME_RECORD);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        PterodactylServer pterodactylServer = gameServerRepository.selectPterodactylServer(context.getPterodactylServerId())
            .orElseThrow(() -> new IllegalStateException("Pterodactyl server not found for subscription: " + context.getSubscriptionId()));
        Context transitionedContext = context.promoteNewPterodactylServerId();
        gameServerRepository.deletePterodactylServer(pterodactylServer.pterodactylServerId());

        pterodactylService.destroyServer(pterodactylServer.pterodactylServerId());

        return transitionService.persistAndProgress(transitionedContext, StepType.PTERODACTYL_ALLOCATION);
    }

}

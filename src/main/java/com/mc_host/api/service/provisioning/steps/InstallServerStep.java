package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.pterodactyl.PterodactylServer;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.provisioning.TransitionService;
import com.mc_host.api.service.resources.PterodactylService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
public class InstallServerStep extends AbstractStep {

	private final PterodactylService pterodactylService;
	private final GameServerRepository gameServerRepository;

    protected InstallServerStep(
		ServerExecutionContextRepository contextRepository,
		TransitionService transitionService,
		PterodactylService pterodactylService,
		GameServerRepository gameServerRepository
    ) {
        super(contextRepository, transitionService);
		this.pterodactylService = pterodactylService;
		this.gameServerRepository = gameServerRepository;
    }

    @Override
    public StepType getType() {
        return StepType.INSTALL_SERVER;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
		String serverUid = gameServerRepository.selectPterodactylServer(context.getNewPterodactylServerId())
			.map(PterodactylServer::pterodactylServerUid)
			.orElseThrow(() -> new IllegalStateException("Pterodactyl server not found: " + context.getNewPterodactylServerId()));;
		pterodactylService.waitForServerAccessible(serverUid, Duration.ofMinutes(5));

		return transitionService.persistAndProgress(context, StepType.CREATE_SUBUSER);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        throw new UnsupportedOperationException("Server Installation step cannot be destroyed directly. Try destroying the Pterodactyl Node step instead.");
    }

}

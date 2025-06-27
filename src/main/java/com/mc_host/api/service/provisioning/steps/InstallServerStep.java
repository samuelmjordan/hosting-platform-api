package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.pterodactyl.PterodactylServer;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.service.resources.PterodactylService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class InstallServerStep extends AbstractStep {

	private final PterodactylService pterodactylService;
	private final GameServerRepository gameServerRepository;

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
		LOGGER.warning("%s step is illegal for destroys. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
		return transitionService.persistAndProgress(context, StepType.PTERODACTYL_SERVER);
    }

}

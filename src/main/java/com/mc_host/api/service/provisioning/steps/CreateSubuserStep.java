package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.pterodactyl.PterodactylServer;
import com.mc_host.api.model.subscription.ContentSubscription;
import com.mc_host.api.model.user.ApplicationUser;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.repository.UserRepository;
import com.mc_host.api.service.resources.PterodactylService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CreateSubuserStep extends AbstractStep {

    private final GameServerRepository gameServerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PterodactylService pterodactylService;

    @Override
    public StepType getType() {
        return StepType.CREATE_SUBUSER;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
		String customerId = subscriptionRepository.selectSubscription(context.getSubscriptionId())
			.map(ContentSubscription::customerId)
			.orElseThrow(() -> new IllegalStateException("Subscription not found: " + context.getSubscriptionId()));
		ApplicationUser user = userRepository.selectUserByCustomerId(customerId)
			.orElseThrow(() -> new IllegalStateException("Customer not found: " + customerId));
		PterodactylServer pterodactylServer = gameServerRepository.selectPterodactylServer(context.getNewPterodactylServerId())
			.orElseThrow(() -> new IllegalStateException("Server not found: " + context.getNewPterodactylServerId()));
		pterodactylService.createSftpSubsuser(user.dummyEmail(), pterodactylServer.pterodactylServerUid());

		//Early return for non-migrations
		if (!context.getMode().isMigrate()) {
			return transitionService.persistAndProgress(context, StepType.START_SERVER);
		}

		//Check for destructive or non-destructive migration
		String oldServerKey = gameServerRepository.selectPterodactylServer(context.getPterodactylServerId())
			.map(PterodactylServer::serverKey)
			.orElse(null);
		if (!Objects.equals(pterodactylServer.serverKey(), oldServerKey)) {
			return transitionService.persistAndProgress(context, StepType.START_SERVER);
		}
		return transitionService.persistAndProgress(context, StepType.TRANSFER_DATA);

    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
		LOGGER.warning("%s step is illegal for destroys. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
		return transitionService.persistAndProgress(context, StepType.PTERODACTYL_SERVER);
    }

}

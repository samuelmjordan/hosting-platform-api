package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.pterodactyl.PterodactylServer;
import com.mc_host.api.model.subscription.ContentSubscription;
import com.mc_host.api.model.user.ApplicationUser;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.repository.UserRepository;
import com.mc_host.api.service.provisioning.TransitionService;
import com.mc_host.api.service.resources.PterodactylService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateSubuserStep extends AbstractStep {

    private final GameServerRepository gameServerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PterodactylService pterodactylService;

    protected CreateSubuserStep(
		ServerExecutionContextRepository contextRepository,
		GameServerRepository gameServerRepository,
		TransitionService transitionService,
        SubscriptionRepository subscriptionRepository,
        UserRepository userRepository,
		PterodactylService pterodactylService
    ) {
        super(contextRepository, transitionService);
        this.gameServerRepository = gameServerRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.userRepository = userRepository;
		this.pterodactylService = pterodactylService;
    }

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
		String pterodactylServerUid = gameServerRepository.selectPterodactylServer(context.getNewPterodactylServerId())
			.map(PterodactylServer::pterodactylServerUid)
			.orElseThrow(() -> new IllegalStateException("Server not found: " + context.getNewPterodactylServerId()));
		pterodactylService.createSftpSubsuser(user.dummyEmail(), pterodactylServerUid);

		if (context.getMode().isMigrate()) {
			return transitionService.persistAndProgress(context, StepType.TRANSFER_DATA);
		}
        return transitionService.persistAndProgress(context, StepType.C_NAME_RECORD);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
		throw new UnsupportedOperationException("Subuser step cannot be destroyed directly. Try destroying the Pterodactyl Server step instead.");
    }

}

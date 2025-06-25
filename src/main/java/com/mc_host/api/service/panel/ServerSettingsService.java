package com.mc_host.api.service.panel;

import com.mc_host.api.client.PterodactylApplicationClient;
import com.mc_host.api.controller.api.subscriptions.panel.ServerSettingsResource;
import com.mc_host.api.model.panel.startup.StartupResponse;
import com.mc_host.api.model.panel.startup.UpdateStartupRequest;
import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.subscription.ContentSubscription;
import com.mc_host.api.queue.JobScheduler;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServerSettingsService implements ServerSettingsResource {

	private final JobScheduler jobScheduler;
	private final SubscriptionRepository subscriptionRepository;
	private final ServerExecutionContextRepository contextRepository;
	private final PterodactylApplicationClient client;

	@Override
	public ResponseEntity<StartupResponse> getSettings(
		String userId,
		String subscriptionId
	) {
		Long serverId = getServerId(subscriptionId);
		PterodactylApplicationClient.PterodactylServerResponse response = client.getServer(serverId);
		return ResponseEntity.ok(mapResponse(response));
	}

	@Override
	public ResponseEntity<StartupResponse> setSettings(
		String userId,
		String subscriptionId,
		UpdateStartupRequest request
	) {
		Long serverId = getServerId(subscriptionId);
		PterodactylApplicationClient.PterodactylServerResponse response =
			client.updateServerStartup(serverId, new PterodactylApplicationClient.PterodactylUpdateStartupRequest(
				request.startupCommand(),
				request.environment(),
				request.egg_id(),
				request.image(),
				false
			));
		return ResponseEntity.ok(mapResponse(response));
	}

	@Override
	public ResponseEntity<Void> reinstallServer(String userId, String subscriptionId) {
		Long serverId = getServerId(subscriptionId);
		client.reinstallServer(serverId);
		return ResponseEntity.ok().build();
	}

	@Override
	public ResponseEntity<Void> recreateServer(String userId, String subscriptionId) {
		contextRepository.newServerKey(subscriptionId);
		String customerId = subscriptionRepository.selectSubscription(subscriptionId)
			.map(ContentSubscription::customerId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404), "Subscription %s not found".formatted(subscriptionId)));
		jobScheduler.scheduleCustomerSubscriptionSync(customerId);
		return ResponseEntity.ok().build();
	}

	private Long getServerId(String subscriptionId) {
		return contextRepository.selectSubscription(subscriptionId)
			.map(Context::getPterodactylServerId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404)));
	}

	private StartupResponse mapResponse(PterodactylApplicationClient.PterodactylServerResponse response) {
		return new StartupResponse(
			response.attributes().container().startup_command(),
			response.attributes().container().image(),
			response.attributes().egg(),
			response.attributes().container().installed(),
			response.attributes().container().environment()
				.entrySet().stream()
				.filter(entry -> !entry.getKey().startsWith("P_"))
				.filter(entry -> !entry.getKey().equals("STARTUP"))
				.collect(Collectors.toMap(
					Map.Entry::getKey,
					Map.Entry::getValue
				))
		);
	}
}

package com.mc_host.api.service;

import com.mc_host.api.client.PterodactylApplicationClient;
import com.mc_host.api.controller.ResourceLimitResource;
import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.server.response.BatchError;
import com.mc_host.api.model.server.response.BatchResourceLimitResponse;
import com.mc_host.api.model.server.response.ResourceLimitResponse;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class ResourceLimitService implements ResourceLimitResource {
	private static final Logger LOGGER = Logger.getLogger(ResourceLimitService.class.getName());

	private final Executor executor = Executors.newVirtualThreadPerTaskExecutor();

	private final PterodactylApplicationClient client;
	private final ServerExecutionContextRepository contextRepository;

	@Override
	public ResponseEntity<ResourceLimitResponse> getProvisioningStatus(
		String userId,
		String subscriptionId
	) {
		Long pterodactylServerId = contextRepository.selectSubscription(subscriptionId)
			.map(Context::getPterodactylServerId)
			.orElseThrow(() -> new ResponseStatusException(
				HttpStatusCode.valueOf(404), "Couldn't fetch subscription context: " + subscriptionId));

		return ResponseEntity.ok(getResourceLimits(userId, subscriptionId, pterodactylServerId));
	}

	@Override
	public ResponseEntity<BatchResourceLimitResponse> getBatchProvisioningStatus(
		String userId,
		List<String> subscriptionIds
	) {
		List<CompletableFuture<BatchResult>> futures = subscriptionIds.stream()
			.map(subscriptionId -> CompletableFuture.supplyAsync(() ->
				processSubscription(userId, subscriptionId), executor))
			.toList();

		List<ResourceLimitResponse> limits = new ArrayList<>();
		List<BatchError> errors = new ArrayList<>();

		for (CompletableFuture<BatchResult> future : futures) {
			BatchResult result = future.join();
			if (result.response != null) {
				limits.add(result.response);
			} else {
				errors.add(result.error);
			}
		}

		return ResponseEntity.ok(new BatchResourceLimitResponse(limits, errors));
	}

	private BatchResult processSubscription(String userId, String subscriptionId) {
		try {
			Optional<Long> pterodactylServerIdOpt = contextRepository.selectSubscription(subscriptionId)
				.map(Context::getPterodactylServerId);

			if (pterodactylServerIdOpt.isEmpty()) {
				return new BatchResult(null, new BatchError(subscriptionId, 404, "Subscription not found"));
			}

			ResourceLimitResponse response = getResourceLimits(userId, subscriptionId, pterodactylServerIdOpt.get());
			return new BatchResult(response, null);

		} catch (Exception e) {
			LOGGER.log(Level.WARNING, String.format("Error processing subscription %s", subscriptionId), e);
			return new BatchResult(null, new BatchError(subscriptionId, 500, "Internal error"));
		}
	}

	private ResourceLimitResponse getResourceLimits(
		String userId,
		String subscriptionId,
		Long pterodactylServerId
	) {
		PterodactylApplicationClient.PterodactylServerResponse pterodactylResponse = client.getServer(pterodactylServerId);
		return new ResourceLimitResponse(
			subscriptionId,
			pterodactylResponse.attributes().limits().memory(),
			pterodactylResponse.attributes().limits().swap(),
			pterodactylResponse.attributes().limits().disk(),
			pterodactylResponse.attributes().limits().io(),
			pterodactylResponse.attributes().limits().cpu(),
			pterodactylResponse.attributes().limits().threads()
		);
	}

	private record BatchResult(
		ResourceLimitResponse response,
		BatchError error
	) {
	}
}
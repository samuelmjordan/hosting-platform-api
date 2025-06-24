package com.mc_host.api.service;

import com.mc_host.api.controller.ProvisioningStatusResource;
import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.Status;
import com.mc_host.api.model.server.ProvisioningStatus;
import com.mc_host.api.model.server.response.BatchError;
import com.mc_host.api.model.server.response.BatchProvisioningStatusResponse;
import com.mc_host.api.model.server.response.ProvisioningStatusResponse;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class ProvisioningStatusService implements ProvisioningStatusResource {
	private static final Logger LOGGER = Logger.getLogger(ProvisioningStatusService.class.getName());

	private final ServerExecutionContextRepository serverExecutionContextRepository;

	@Override
	public ResponseEntity<ProvisioningStatusResponse> getProvisioningStatus(String userId, String subscriptionId) {
		Context context = serverExecutionContextRepository.selectSubscription(subscriptionId)
			.orElseThrow(() -> new ResponseStatusException(
				HttpStatusCode.valueOf(404), "Couldn't fetch subscription context: " + subscriptionId));

		Optional<ProvisioningStatus> status = getStatusFromContext(context, subscriptionId);
		if (status.isEmpty()) {
			LOGGER.log(Level.SEVERE, String.format("Failed to get server status: %s", subscriptionId));
			return ResponseEntity.internalServerError().build();
		}

		return ResponseEntity.ok(new ProvisioningStatusResponse(subscriptionId, status.get()));
	}

	@Override
	public ResponseEntity<BatchProvisioningStatusResponse> getBatchProvisioningStatus(String userId, List<String> subscriptionIds) {
		List<ProvisioningStatusResponse> statuses = new ArrayList<>();
		List<BatchError> errors = new ArrayList<>();

		for (String subscriptionId : subscriptionIds) {
			try {
				Optional<Context> contextOpt = serverExecutionContextRepository.selectSubscription(subscriptionId);
				if (contextOpt.isEmpty()) {
					errors.add(new BatchError(subscriptionId, 404, "Subscription not found"));
					continue;
				}

				getStatusFromContext(contextOpt.get(), subscriptionId)
					.ifPresentOrElse(
						status -> statuses.add(new ProvisioningStatusResponse(subscriptionId, status)),
						() -> errors.add(new BatchError(subscriptionId, 500,"Failed to determine status"))
					);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, String.format("Error processing subscription %s: %s", subscriptionId, e.getMessage()));
				errors.add(new BatchError(subscriptionId, 500, "Internal error"));
			}
		}

		return ResponseEntity.ok(new BatchProvisioningStatusResponse(statuses, errors));
	}

	private Optional<ProvisioningStatus> getStatusFromContext(Context context, String subscriptionId) {
		if (context.isIllegalState()) {
			LOGGER.log(Level.SEVERE, String.format("Server is in an illegal state: %s", subscriptionId));
			return Optional.of(ProvisioningStatus.FAILED);
		}
		if (context.isCreated()) {
			return Optional.of(ProvisioningStatus.READY);
		}
		if (context.getMode().isCreate()) {
			return Optional.of(ProvisioningStatus.PROVISIONING);
		}
		if (context.getMode().isMigrate()) {
			return Optional.of(ProvisioningStatus.MIGRATING);
		}
		if (context.isDestroyed()) {
			return Optional.of(ProvisioningStatus.INACTIVE);
		}
		if (context.getMode().isDestroy()) {
			return Optional.of(ProvisioningStatus.DESTROYING);
		}
		if (context.getStatus().equals(Status.FAILED)) {
			return Optional.of(ProvisioningStatus.FAILED);
		}
		return Optional.empty();
	}
}
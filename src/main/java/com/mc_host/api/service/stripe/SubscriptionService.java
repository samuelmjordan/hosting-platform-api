package com.mc_host.api.service.stripe;

import com.mc_host.api.client.PterodactylApplicationClient;
import com.mc_host.api.controller.api.subscriptions.SubscriptionController;
import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.Mode;
import com.mc_host.api.model.provisioning.Status;
import com.mc_host.api.model.server.ProvisioningStatus;
import com.mc_host.api.model.server.response.ProvisioningStatusResponse;
import com.mc_host.api.model.server.response.ResourceLimitResponse;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class SubscriptionService implements SubscriptionController {
    private static final Logger LOGGER = Logger.getLogger(SubscriptionService.class.getName());

    private final PterodactylApplicationClient pterodactylApplicationClient;
    private final ServerExecutionContextRepository contextRepository;
    private final ServerExecutionContextRepository serverExecutionContextRepository;

    @Override
    public ResponseEntity<ProvisioningStatusResponse> getProvisioningStatus(
        String subscriptionId
    ) {
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
    public ResponseEntity<ResourceLimitResponse> getResourceLimits(
        String subscriptionId
    ) {
        Long pterodactylServerId = contextRepository.selectSubscription(subscriptionId)
            .map(Context::getPterodactylServerId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatusCode.valueOf(404), "Couldn't fetch subscription context: " + subscriptionId));

        return ResponseEntity.ok(getResourceLimits(subscriptionId, pterodactylServerId));
    }

    private ResourceLimitResponse getResourceLimits(
        String subscriptionId,
        Long pterodactylServerId
    ) {
        PterodactylApplicationClient.PterodactylServerResponse pterodactylResponse = pterodactylApplicationClient.getServer(pterodactylServerId);
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

    private Optional<ProvisioningStatus> getStatusFromContext(
        Context context,
        String subscriptionId
    ) {
        if (context.isIllegalState()) {
            LOGGER.log(Level.SEVERE, String.format("Server is in an illegal state: %s", subscriptionId));
            return Optional.of(ProvisioningStatus.FAILED);
        }
        if (context.getStatus().equals(Status.FAILED)) {
            return Optional.of(ProvisioningStatus.FAILED);
        }
        if (context.isCreated()) {
            return Optional.of(ProvisioningStatus.READY);
        }
        if (context.isDestroyed()) {
            return Optional.of(ProvisioningStatus.INACTIVE);
        }
        if (context.getMode().equals(Mode.CREATE)) {
            return Optional.of(ProvisioningStatus.PROVISIONING);
        }
        if (context.getMode().isMigrate()) {
            return Optional.of(ProvisioningStatus.MIGRATING);
        }
        if (context.getMode().equals(Mode.DESTROY)) {
            return Optional.of(ProvisioningStatus.DESTROYING);
        }
        return Optional.empty();
    }
}
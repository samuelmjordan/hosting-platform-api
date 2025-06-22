package com.mc_host.api.service.panel;

import com.mc_host.api.controller.panel.SftpController;
import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.resource.pterodactyl.PterodactylServer;
import com.mc_host.api.model.user.ApplicationUser;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.repository.UserRepository;
import com.mc_host.api.service.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SftpService implements SftpController {

	private final EncryptionService encryptionService;
	private final UserRepository userRepository;
	private final ServerExecutionContextRepository contextRepository;
	private final GameServerRepository gameServerRepository;

	@Override
	public ResponseEntity<CredentialsResponse> getCredentials(String userId, String subscriptionId, String directory) {
		ApplicationUser user = userRepository.selectUser(userId)
			.orElseThrow(() -> new IllegalStateException("User %s does not exist".formatted(userId)));
		Long serverId = contextRepository.selectSubscription(subscriptionId)
			.map(Context::getPterodactylServerId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404), "Subscription %s not found".formatted(subscriptionId)));
		PterodactylServer server = gameServerRepository.selectPterodactylServer(serverId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404), "No server found for subscription %s".formatted(subscriptionId)));
		return ResponseEntity.ok(new CredentialsResponse(
			String.join(".", user.pterodactylUsername(), server.pterodactylServerUid().split("-")[0]),
			encryptionService.encrypt(user.pterodactylPassword())
		));
	}
}

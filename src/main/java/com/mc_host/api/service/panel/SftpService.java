package com.mc_host.api.service.panel;

import com.mc_host.api.controller.panel.SftpController;
import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.resource.dns.DnsARecord;
import com.mc_host.api.model.resource.pterodactyl.PterodactylServer;
import com.mc_host.api.model.user.ApplicationUser;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.NodeRepository;
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
	private final NodeRepository nodeRepository;

	@Override
	public ResponseEntity<CredentialsResponse> getCredentials(String userId, String subscriptionId) {
		ApplicationUser user = userRepository.selectUser(userId)
			.orElseThrow(() -> new IllegalStateException("User %s does not exist".formatted(userId)));
		Context context = contextRepository.selectSubscription(subscriptionId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404), "Subscription %s not found".formatted(subscriptionId)));
		String serverUid = gameServerRepository.selectPterodactylServer(context.getPterodactylServerId())
			.map(PterodactylServer::pterodactylServerUid)
			.orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404), "No server found for subscription %s".formatted(subscriptionId)));
		String url = nodeRepository.selectDnsARecord(context.getARecordId())
			.map(DnsARecord::recordName)
			.orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404), "No A record found for subscription %s".formatted(subscriptionId)));
		return ResponseEntity.ok(new CredentialsResponse(
			"sftp://" + url,
			String.join(".", user.pterodactylUsername(), serverUid.split("-")[0]),
			encryptionService.encrypt(user.pterodactylPassword()),
			2022
		));
	}
}

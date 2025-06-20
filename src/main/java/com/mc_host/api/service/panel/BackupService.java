package com.mc_host.api.service.panel;

import com.mc_host.api.client.PterodactylUserClient;
import com.mc_host.api.controller.panel.BackupResource;
import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.resource.pterodactyl.PterodactylServer;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BackupService implements BackupResource {

	private final PterodactylUserClient pterodactylClient;
	private final ServerExecutionContextRepository serverExecutionContextRepository;
	private final GameServerRepository gameServerRepository;

	public BackupService(
		PterodactylUserClient pterodactylClient,
		ServerExecutionContextRepository serverExecutionContextRepository,
		GameServerRepository gameServerRepository
	) {
		this.pterodactylClient = pterodactylClient;
		this.serverExecutionContextRepository = serverExecutionContextRepository;
		this.gameServerRepository = gameServerRepository;
	}

	@Override
	public ResponseEntity<?> listBackups(String userId, String subscriptionId) {
		var serverUuid = resolveServerUid(userId, subscriptionId);
		var backups = pterodactylClient.listBackups(serverUuid);
		return ResponseEntity.ok(backups);
	}

	@Override
	public ResponseEntity<?> createBackup(String userId, String subscriptionId, CreateBackupRequest request) {
		var serverUuid = resolveServerUid(userId, subscriptionId);
		var backup = request != null && request.name() != null
			? pterodactylClient.createBackup(serverUuid, request.name())
			: pterodactylClient.createBackup(serverUuid);
		return ResponseEntity.ok(backup);
	}

	@Override
	public ResponseEntity<?> getBackupDetails(String userId, String subscriptionId, String backupId) {
		var serverUuid = resolveServerUid(userId, subscriptionId);
		var backup = pterodactylClient.getBackupDetails(serverUuid, backupId);
		return ResponseEntity.ok(backup);
	}

	@Override
	public ResponseEntity<?> getBackupDownloadLink(String userId, String subscriptionId, String backupId) {
		var serverUuid = resolveServerUid(userId, subscriptionId);
		var downloadLink = pterodactylClient.getBackupDownloadLink(serverUuid, backupId);
		return ResponseEntity.ok(downloadLink);
	}

	@Override
	public ResponseEntity<?> restoreBackup(String userId, String subscriptionId, String backupId) {
		var serverUuid = resolveServerUid(userId, subscriptionId);
		pterodactylClient.restoreBackup(serverUuid, backupId);
		return ResponseEntity.noContent().build();
	}

	@Override
	public ResponseEntity<Void> deleteBackup(String userId, String subscriptionId, String backupId) {
		var serverUuid = resolveServerUid(userId, subscriptionId);
		pterodactylClient.deleteBackup(serverUuid, backupId);
		return ResponseEntity.noContent().build();
	}

	private String resolveServerUid(String userId, String subscriptionId) {
		Long serverId = serverExecutionContextRepository.selectSubscription(subscriptionId)
			.map(Context::getPterodactylServerId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404)));
		return gameServerRepository.selectPterodactylServer(serverId)
			.map(PterodactylServer::pterodactylServerUid)
			.orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404)));
	}
}
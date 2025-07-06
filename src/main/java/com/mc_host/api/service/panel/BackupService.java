package com.mc_host.api.service.panel;

import com.mc_host.api.client.PterodactylUserClient;
import com.mc_host.api.controller.api.subscriptions.panel.BackupController;
import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.resource.pterodactyl.PterodactylServer;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BackupService implements BackupController {

	private final PterodactylUserClient pterodactylClient;
	private final ServerExecutionContextRepository serverExecutionContextRepository;
	private final GameServerRepository gameServerRepository;

	@Override
	public ResponseEntity<List<Backup>> listBackups(String subscriptionId) {
		String serverUuid = resolveServerUid(subscriptionId);
		List<Backup> backups = pterodactylClient.listBackups(serverUuid)
			.data().stream()
			.map(backupObject -> mapToBackup(backupObject.attributes()))
			.toList();
		return ResponseEntity.ok(backups);
	}

	@Override
	public ResponseEntity<Backup> createBackup(String subscriptionId, String name) {
		String serverUuid = resolveServerUid(subscriptionId);
		Backup backup = mapToBackup((
			name != null
			? pterodactylClient.createBackup(serverUuid, name)
			: pterodactylClient.createBackup(serverUuid)
		).attributes());
		return ResponseEntity.ok(backup);
	}

	@Override
	public ResponseEntity<Backup> getBackupDetails(String subscriptionId, String backupId) {
		String serverUuid = resolveServerUid(subscriptionId);
		Backup backup = mapToBackup(pterodactylClient.getBackupDetails(serverUuid, backupId).attributes());
		return ResponseEntity.ok(backup);
	}

	@Override
	public ResponseEntity<String> getBackupDownloadLink(String subscriptionId, String backupId) {
		String serverUuid = resolveServerUid(subscriptionId);
		String downloadLink = pterodactylClient.getBackupDownloadLink(serverUuid, backupId).attributes().url();
		return ResponseEntity.ok(downloadLink);
	}

	@Override
	public ResponseEntity<Void> restoreBackup(String subscriptionId, String backupId) {
		String serverUuid = resolveServerUid(subscriptionId);
		pterodactylClient.restoreBackup(serverUuid, backupId);
		return ResponseEntity.noContent().build();
	}

	@Override
	public ResponseEntity<Void> deleteBackup(String subscriptionId, String backupId) {
		String serverUuid = resolveServerUid(subscriptionId);
		pterodactylClient.deleteBackup(serverUuid, backupId);
		return ResponseEntity.noContent().build();
	}

	private String resolveServerUid(String subscriptionId) {
		Long serverId = serverExecutionContextRepository.selectSubscription(subscriptionId)
			.map(Context::getPterodactylServerId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404)));
		return gameServerRepository.selectPterodactylServer(serverId)
			.map(PterodactylServer::pterodactylServerUid)
			.orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404)));
	}

	private Backup mapToBackup(PterodactylUserClient.BackupAttributes backupResponse) {
		return new Backup(
			backupResponse.uuid(),
			backupResponse.name(),
			backupResponse.ignored_files(),
			backupResponse.sha256_hash(),
			backupResponse.bytes(),
			Instant.parse(backupResponse.created_at()),
			Optional.ofNullable(backupResponse.completed_at())
				.map(Instant::parse)
				.orElse(null)
		);
	}
}
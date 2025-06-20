package com.mc_host.api.service.panel;

import com.mc_host.api.client.PterodactylUserClient;
import com.mc_host.api.model.panel.request.transfer.TempFileMultipartFile;
import com.mc_host.api.model.resource.pterodactyl.PterodactylServer;
import com.mc_host.api.model.resource.pterodactyl.file.FileObject;
import com.mc_host.api.repository.GameServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class TransferService {
	private final static Logger LOGGER = Logger.getLogger(TransferService.class.getName());
	private final static long MAX_BACKUP_SIZE_BYTES = 1_000_000 * 1024; // 1gb
	private final static int BACKUP_TIMEOUT_ATTEMPTS = 60;
	private final static int BACKUP_POLL_INTERVAL_SECONDS = 5;

	private final PterodactylUserClient pterodactylClient;
	private final GameServerRepository gameServerRepository;

	public void transferServerData(Long sourceServerId, Long targetServerId) throws Exception {
		LOGGER.info("starting server data transfer from %s to %s".formatted(sourceServerId, targetServerId));

		var sourceServer = getServer(sourceServerId);
		var targetServer = getServer(targetServerId);

		var backupUid = createAndWaitForBackup(sourceServer.pterodactylServerUid());

		try {
			var tempFile = downloadBackupToTempFile(sourceServer.pterodactylServerUid(), backupUid);
			try {
				uploadBackupToTarget(targetServer.pterodactylServerUid(), tempFile);
				decompressBackupOnTarget(targetServer.pterodactylServerUid());
			} finally {
				cleanupTempFile(tempFile);
			}
		} finally {
			cleanupSourceBackup(sourceServer.pterodactylServerUid(), backupUid);
		}

		LOGGER.info("server data transfer completed successfully");
	}

	private String createAndWaitForBackup(String sourceServerUid) throws Exception {
		LOGGER.info("creating backup on source server %s".formatted(sourceServerUid));

		var backupResponse = pterodactylClient.createBackup(
			sourceServerUid,
			"transfer-backup-" + System.currentTimeMillis()
		);

		var backupUid = backupResponse.attributes().uuid();

		if (backupResponse.attributes().bytes() >= MAX_BACKUP_SIZE_BYTES) {
			throw new IllegalStateException("backup too large: %d bytes".formatted(backupResponse.attributes().bytes()));
		}

		waitForBackupCompletion(sourceServerUid, backupUid);
		return backupUid;
	}

	private Path downloadBackupToTempFile(String sourceServerUid, String backupUid) throws Exception {
		LOGGER.info("getting download link for backup %s".formatted(backupUid));
		var downloadUrl = pterodactylClient.getBackupDownloadLink(sourceServerUid, backupUid);

		var tempFile = Files.createTempFile("server-transfer-", ".tar.gz");
		LOGGER.info("created temp file %s for transfer".formatted(tempFile));

		var httpClient = HttpClient.newHttpClient();
		var downloadRequest = HttpRequest.newBuilder()
			.uri(URI.create(downloadUrl.attributes().url()))
			.GET()
			.build();

		LOGGER.info("downloading backup file");
		httpClient.send(downloadRequest, HttpResponse.BodyHandlers.ofFile(tempFile));

		return tempFile;
	}

	private void uploadBackupToTarget(String targetServerUid, Path tempFile) throws Exception {
		var multipartFile = new TempFileMultipartFile("files", "transfer-backup.tar.gz", tempFile);

		clearServer(targetServerUid);

		LOGGER.info("uploading backup to target server");
		pterodactylClient.uploadFile(targetServerUid, multipartFile);
	}

	private void decompressBackupOnTarget(String targetServerUid) {
		LOGGER.info("decompressing backup on target server %s".formatted(targetServerUid));
		pterodactylClient.decompressFile(targetServerUid, "/", "transfer-backup.tar.gz");
	}

	private void cleanupTempFile(Path tempFile) {
		if (tempFile != null) {
			try {
				Files.deleteIfExists(tempFile);
				LOGGER.info("cleaned up temp file %s".formatted(tempFile));
			} catch (Exception e) {
				LOGGER.warning("failed to cleanup temp file %s: %s".formatted(tempFile, e.getMessage()));
			}
		}
	}

	private void cleanupSourceBackup(String sourceServerUid, String backupUid) {
		try {
			LOGGER.info("cleaning up backup from source server");
			pterodactylClient.deleteBackup(sourceServerUid, backupUid);
		} catch (Exception e) {
			LOGGER.warning("failed to cleanup source backup %s: %s".formatted(backupUid, e.getMessage()));
		}
	}

	private void waitForBackupCompletion(String serverUid, String backupUid) throws InterruptedException {
		LOGGER.info("waiting for backup %s to complete".formatted(backupUid));

		for (int attempts = 0; attempts < BACKUP_TIMEOUT_ATTEMPTS; attempts++) {
			var backup = pterodactylClient.getBackupDetails(serverUid, backupUid);
			var completedAt = backup.attributes().completed_at();

			if (completedAt != null && !completedAt.isEmpty()) {
				LOGGER.info("backup completed at %s".formatted(completedAt));
				return;
			}

			LOGGER.info("backup still in progress, attempt %s/%d".formatted(attempts + 1, BACKUP_TIMEOUT_ATTEMPTS));
			TimeUnit.SECONDS.sleep(BACKUP_POLL_INTERVAL_SECONDS);
		}

		throw new RuntimeException("backup did not complete within timeout");
	}

	private void clearServer(String serverUid) {
		List<String> fileNames = pterodactylClient.listFiles(serverUid, "/").stream()
			.map(FileObject::attributes)
			.map(FileObject.FileAttributes::name)
			.toList();
		if (fileNames.isEmpty()) {
			return;
		}
		pterodactylClient.deleteFiles(serverUid, "/", fileNames);
	}

	private PterodactylServer getServer(Long serverId) {
		return gameServerRepository.selectPterodactylServer(serverId)
			.orElseThrow(() -> new ResponseStatusException(
				HttpStatusCode.valueOf(404),
				"server %s not found".formatted(serverId)
			));
	}
}
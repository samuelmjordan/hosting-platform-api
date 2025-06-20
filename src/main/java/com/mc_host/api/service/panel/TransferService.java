package com.mc_host.api.service.panel;

import com.mc_host.api.client.PterodactylUserClient;
import com.mc_host.api.model.panel.request.transfer.TempFileMultipartFile;
import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.resource.pterodactyl.PterodactylServer;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class TransferService {
	private final static Logger LOGGER = Logger.getLogger(TransferService.class.getName());

	private final PterodactylUserClient pterodactylClient;
	private final ServerExecutionContextRepository serverExecutionContextRepository;
	private final GameServerRepository gameServerRepository;
	private final FileService fileService;

	public void transferServerData(String sourceSubscriptionId, String targetSubscriptionId) throws Exception {
		LOGGER.info("Starting server data transfer from %s to %s".formatted(sourceSubscriptionId, targetSubscriptionId));

		var sourceServerUid = getServerUid(sourceSubscriptionId);
		var targetServerUid = getServerUid(targetSubscriptionId);

		// create backup on source server
		LOGGER.info("Creating backup on source server %s".formatted(sourceServerUid));
		var backupResponse = pterodactylClient.createBackup(sourceServerUid, "transfer-backup-" + System.currentTimeMillis());
		var backupUuid = backupResponse.attributes().uuid();

		// wait for backup to complete
		waitForBackupCompletion(sourceServerUid, backupUuid);

		// get download link
		LOGGER.info("Getting download link for backup %s".formatted(backupUuid));
		var downloadUrl = pterodactylClient.getBackupDownloadLink(sourceServerUid, backupUuid);

		// stream transfer the backup
		transferBackupFile(downloadUrl.attributes().url(), targetSubscriptionId);

		// decompress on target server
		LOGGER.info("Decompressing backup on target server %s".formatted(targetServerUid));
		pterodactylClient.decompressFile(targetServerUid, "/", "transfer-backup.tar.gz");

		// cleanup backup from source
		LOGGER.info("Cleaning up backup from source server");
		pterodactylClient.deleteBackup(sourceServerUid, backupUuid);

		LOGGER.info("Server data transfer completed successfully");
	}

	private void waitForBackupCompletion(String serverUid, String backupUuid) throws InterruptedException {
		LOGGER.info("Waiting for backup %s to complete".formatted(backupUuid));

		for (int attempts = 0; attempts < 60; attempts++) { // max 10 minutes
			var backup = pterodactylClient.getBackupDetails(serverUid, backupUuid);
			var completedAt = backup.attributes().completed_at();

			if (completedAt != null && !completedAt.isEmpty()) {
				LOGGER.info("Backup completed at %s".formatted(completedAt));
				return;
			}

			LOGGER.info("Backup still in progress, attempt %s/60".formatted(attempts + 1));
			TimeUnit.SECONDS.sleep(10);
		}

		throw new RuntimeException("Backup did not complete within timeout");
	}

	private void transferBackupFile(String downloadUrl, String targetSubscriptionId) throws IOException, InterruptedException {
		Path tempFile = null;

		try {
			// create temp file for streaming
			tempFile = Files.createTempFile("server-transfer-", ".tar.gz");
			LOGGER.info("Created temp file %s for transfer".formatted(tempFile));

			// stream download to temp file
			var httpClient = HttpClient.newHttpClient();
			var downloadRequest = HttpRequest.newBuilder()
				.uri(URI.create(downloadUrl))
				.GET()
				.build();

			LOGGER.info("Downloading backup file");
			httpClient.send(downloadRequest, HttpResponse.BodyHandlers.ofFile(tempFile));

			// create multipart file from temp file
			var multipartFile = new TempFileMultipartFile("files", "transfer-backup.tar.gz", tempFile);

			// upload to target server
			LOGGER.info("Uploading backup to target server");
			fileService.uploadFile("system", targetSubscriptionId, multipartFile);

		} finally {
			// cleanup temp file
			if (tempFile != null) {
				Files.deleteIfExists(tempFile);
				LOGGER.info("Cleaned up temp file %s".formatted(tempFile));
			}
		}
	}

	private String getServerUid(String subscriptionId) {
		Long serverId = serverExecutionContextRepository.selectSubscription(subscriptionId)
			.map(Context::getPterodactylServerId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404), "Subscription %s not found".formatted(subscriptionId)));
		return gameServerRepository.selectPterodactylServer(serverId)
			.map(PterodactylServer::pterodactylServerUid)
			.orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404), "Server %s not found".formatted(serverId)));
	}
}
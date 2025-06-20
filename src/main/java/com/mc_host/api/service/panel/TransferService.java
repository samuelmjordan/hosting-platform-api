package com.mc_host.api.service.panel;

import com.mc_host.api.configuration.PterodactylConfiguration;
import com.mc_host.api.model.resource.pterodactyl.PterodactylAllocation;
import com.mc_host.api.model.resource.pterodactyl.PterodactylServer;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class TransferService {
	private final static Logger LOGGER = Logger.getLogger(TransferService.class.getName());

	private final PterodactylConfiguration pterodactylConfiguration;
	private final GameServerRepository gameServerRepository;
	private final NodeRepository nodeRepository;

	public void transferServerDataViaSftp(Long sourceServerId, Long targetServerId) throws Exception {
		LOGGER.info("starting sshj sftp transfer from %s to %s".formatted(sourceServerId, targetServerId));

		var sourceSftpDetails = getSftpDetails(sourceServerId);
		var targetSftpDetails = getSftpDetails(targetServerId);

		LOGGER.info(sourceSftpDetails.toString());
		LOGGER.info(targetSftpDetails.toString());

		try (var sourceClient = createSshClient(sourceSftpDetails);
			 var targetClient = createSshClient(targetSftpDetails);
			 var sourceSftp = sourceClient.newSFTPClient();
			 var targetSftp = targetClient.newSFTPClient()) {

			// clear target first
			clearServerViaSftp(targetSftp, "/");

			// transfer everything recursively
			transferDirectoryRecursive(sourceSftp, targetSftp, "/", "/");

			LOGGER.info("sshj sftp transfer completed successfully");
		}
	}

	private SSHClient createSshClient(SftpDetails details) throws IOException {
		SSHClient client = new SSHClient();
		client.addHostKeyVerifier(new PromiscuousVerifier());
		client.connect(details.host, details.port);
		client.authPassword(details.username, details.password);
		return client;
	}

	private void transferDirectoryRecursive(SFTPClient source, SFTPClient target,
											String sourcePath, String targetPath) throws IOException {
		LOGGER.info("transferring directory %s to %s".formatted(sourcePath, targetPath));

		// ensure target directory exists
		createDirectoryIfNotExists(target, targetPath);

		var entries = source.ls(sourcePath);

		for (var entry : entries) {
			String filename = entry.getName();

			if (".".equals(filename) || "..".equals(filename)) {
				continue;
			}

			String sourceFilePath = buildPath(sourcePath, filename);
			String targetFilePath = buildPath(targetPath, filename);

			if (entry.isDirectory()) {
				transferDirectoryRecursive(source, target, sourceFilePath, targetFilePath);
			} else {
				LOGGER.info("transferring file %s (%d bytes)".formatted(sourceFilePath, entry.getAttributes().getSize()));
				try {
					Path tempFile = Files.createTempFile("sftp-transfer-", ".tmp");
					try {
						source.get(sourceFilePath, tempFile.toString());
						target.put(tempFile.toString(), targetFilePath);
					} finally {
						Files.deleteIfExists(tempFile);
					}
				} catch (Exception e) {
					LOGGER.warning("failed to transfer file %s: %s".formatted(sourceFilePath, e.getMessage()));
					throw new RuntimeException("file transfer failed for " + sourceFilePath, e);
				}
			}
		}
	}

	private void createDirectoryIfNotExists(SFTPClient client, String path) throws IOException {
		if (path.equals("/")) return; // root always exists

		try {
			client.stat(path);
		} catch (Exception e) {
			// directory doesn't exist, create parent first then this one
			String parentPath = path.substring(0, path.lastIndexOf('/'));
			if (!parentPath.isEmpty()) {
				createDirectoryIfNotExists(client, parentPath);
			}

			try {
				client.mkdir(path);
				LOGGER.info("created directory %s".formatted(path));
			} catch (Exception ex) {
				// might have been created by another thread, check if it exists now
				try {
					client.stat(path);
				} catch (Exception stillFails) {
					throw new RuntimeException("failed to create directory " + path, ex);
				}
			}
		}
	}

	private void clearServerViaSftp(SFTPClient client, String path) throws IOException {
		LOGGER.info("clearing server directory %s".formatted(path));

		var entries = client.ls(path);

		for (var entry : entries) {
			String filename = entry.getName();

			if (".".equals(filename) || "..".equals(filename)) {
				continue;
			}

			String filePath = buildPath(path, filename);

			try {
				if (entry.isDirectory()) {
					// recursively clear and remove directory
					clearServerViaSftp(client, filePath);
					client.rmdir(filePath);
					LOGGER.info("removed directory %s".formatted(filePath));
				} else {
					// remove file
					client.rm(filePath);
					LOGGER.info("removed file %s".formatted(filePath));
				}
			} catch (Exception e) {
				LOGGER.warning("failed to remove %s: %s".formatted(filePath, e.getMessage()));
				// continue with other files instead of failing completely
			}
		}
	}

	private String buildPath(String parent, String child) {
		if (parent.endsWith("/")) {
			return parent + child;
		}
		return parent + "/" + child;
	}

	private SftpDetails getSftpDetails(Long serverId) {
		PterodactylServer server = gameServerRepository.selectPterodactylServer(serverId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404), "Server %s not found".formatted(serverId)));
		PterodactylAllocation allocation = nodeRepository.selectPterodactylAllocation(server.allocationId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404), "Allocation %s not found".formatted(serverId)));
		return new SftpDetails(
			allocation.ip(),
			2022,
			"%s.%s".formatted(
				pterodactylConfiguration.getAdminUser(),
				server.pterodactylServerUid().split("-")[0]
			),
			pterodactylConfiguration.getAdminPassword()
		);
	}

	private record SftpDetails(String host, int port, String username, String password) {}
}
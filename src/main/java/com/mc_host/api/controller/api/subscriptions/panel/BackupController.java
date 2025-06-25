package com.mc_host.api.controller.api.subscriptions.panel;

import com.mc_host.api.auth.ValidatedSubscription;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("api/panel/user/subscription/{subscriptionId}/backup")
public interface BackupController {

	@GetMapping
	ResponseEntity<List<Backup>> listBackups(
		@ValidatedSubscription String subscriptionId
	);

	@PostMapping
	ResponseEntity<Backup> createBackup(
		@ValidatedSubscription String subscriptionId,
		@RequestParam(required = false) String name
	);

	@GetMapping("{backupId}")
	ResponseEntity<Backup> getBackupDetails(
		@ValidatedSubscription String subscriptionId,
		@PathVariable String backupId
	);

	@GetMapping("{backupId}/download")
	ResponseEntity<String> getBackupDownloadLink(
		@ValidatedSubscription String subscriptionId,
		@PathVariable String backupId
	);

	@PostMapping("{backupId}/restore")
	ResponseEntity<Void> restoreBackup(
		@ValidatedSubscription String subscriptionId,
		@PathVariable String backupId
	);

	@DeleteMapping("{backupId}")
	ResponseEntity<Void> deleteBackup(
		@ValidatedSubscription String subscriptionId,
		@PathVariable String backupId
	);

	record Backup(
		String id,
		String name,
		List<String> ignoredFiles,
		String hash,
		long bytes,
		Instant createdAt,
		Instant completedAt
	) {}
}
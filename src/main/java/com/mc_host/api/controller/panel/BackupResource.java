package com.mc_host.api.controller.panel;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/panel/user/{userId}/subscription/{subscriptionId}/backup")
public interface BackupResource {

	@GetMapping
	ResponseEntity<?> listBackups(
		@PathVariable String userId,
		@PathVariable String subscriptionId
	);

	@PostMapping
	ResponseEntity<?> createBackup(
		@PathVariable String userId,
		@PathVariable String subscriptionId,
		@RequestBody(required = false) CreateBackupRequest request
	);

	@GetMapping("/{backupId}")
	ResponseEntity<?> getBackupDetails(
		@PathVariable String userId,
		@PathVariable String subscriptionId,
		@PathVariable String backupId
	);

	@GetMapping("/{backupId}/download")
	ResponseEntity<?> getBackupDownloadLink(
		@PathVariable String userId,
		@PathVariable String subscriptionId,
		@PathVariable String backupId
	);

	@PostMapping("/{backupId}/restore")
	ResponseEntity<?> restoreBackup(
		@PathVariable String userId,
		@PathVariable String subscriptionId,
		@PathVariable String backupId
	);

	@DeleteMapping("/{backupId}")
	ResponseEntity<Void> deleteBackup(
		@PathVariable String userId,
		@PathVariable String subscriptionId,
		@PathVariable String backupId
	);

	// request dto
	record CreateBackupRequest(String name) {}
}
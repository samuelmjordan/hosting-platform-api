package com.mc_host.api.controller.panel;

import com.mc_host.api.service.panel.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/panel/transfer")
@RequiredArgsConstructor
public class TransferController {
	private final static Logger LOGGER = Logger.getLogger(TransferController.class.getName());

	private final TransferService transferService;

	@PostMapping("/server-data")
	public ResponseEntity<?> transferServerData(@RequestBody TransferRequest request) throws Exception {
		LOGGER.info("Received transfer request from %s to %s".formatted(request.sourceId(), request.targetId()));

		transferService.transferServerDataViaSftp(request.sourceId(), request.targetId());

		return ResponseEntity.accepted().body(Map.of(
			"message", "Transfer finished"
		));
	}

	// request/response records
	public record TransferRequest(
		Long sourceId,
		Long targetId
	) {}

}
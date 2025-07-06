package com.mc_host.api.controller.data;

import com.mc_host.api.model.resource.pterodactyl.games.EggDefinition;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("egg")
public interface EggController {

	@GetMapping
	public ResponseEntity<List<EggDefinition>> getEggs();
}

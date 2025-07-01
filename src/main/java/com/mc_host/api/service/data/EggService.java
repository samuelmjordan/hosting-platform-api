package com.mc_host.api.service.data;

import com.mc_host.api.controller.data.EggController;
import com.mc_host.api.model.resource.pterodactyl.games.Egg;
import com.mc_host.api.model.resource.pterodactyl.games.EggDefinition;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class EggService implements EggController {

	@Override
	public ResponseEntity<List<EggDefinition>> getEggs() {
		List<EggDefinition> eggs = Arrays.stream(Egg.values()).map(Egg::getDefinition).toList();
		System.out.println(eggs.size());
		return ResponseEntity.ok(eggs);
	}
}

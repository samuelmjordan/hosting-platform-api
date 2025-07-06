package com.mc_host.api.model.resource.pterodactyl.games;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class EggTypeInitializer {
	@PostConstruct
	public void init() {
		Egg.preloadAllDefinitions();
	}
}

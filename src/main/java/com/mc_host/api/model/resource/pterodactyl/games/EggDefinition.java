package com.mc_host.api.model.resource.pterodactyl.games;

import java.util.List;

public record EggDefinition(
	Long id,
	Egg type,
	String name,
	String description,
	List<String> dockerImages,
	String startup,
	List<EggVariable> variables
) {
}

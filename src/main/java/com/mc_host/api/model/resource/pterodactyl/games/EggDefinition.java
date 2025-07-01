package com.mc_host.api.model.resource.pterodactyl.games;

import java.util.List;
import java.util.Map;

public record EggDefinition(
	Long id,
	Egg type,
	String name,
	String description,
	Map<String, String> dockerImages,
	String startup,
	List<EggVariable> variables
) {
}

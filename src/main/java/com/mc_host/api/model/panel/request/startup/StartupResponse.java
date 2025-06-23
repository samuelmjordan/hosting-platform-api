package com.mc_host.api.model.panel.request.startup;

import com.mc_host.api.model.resource.pterodactyl.games.Egg;

import java.util.Map;

public record StartupResponse(
	String startupCommand,
	String image,
	Egg egg,
	Boolean installed,
	Map<String, String> environment
) {}
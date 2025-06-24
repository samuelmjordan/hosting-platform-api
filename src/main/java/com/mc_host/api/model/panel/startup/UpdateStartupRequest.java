package com.mc_host.api.model.panel.startup;

import java.util.Map;

public record UpdateStartupRequest(
	String startupCommand,
	Map<String, String> environment,
	Long egg_id,
	String image
) {}
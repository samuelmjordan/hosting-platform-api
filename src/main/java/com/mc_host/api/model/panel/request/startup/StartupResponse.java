package com.mc_host.api.model.panel.request.startup;

import java.util.Map;

public record StartupResponse(
	String startupCommand,
	String image,
	Long eggId,
	Boolean installed,
	Map<String, String> environment
) {}
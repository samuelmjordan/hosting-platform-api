package com.mc_host.api.model.resource.pterodactyl.games;

public record EggVariable(
	String name,
	String description,
	String envVariable,
	String defaultValue,
	boolean userViewable,
	boolean userEditable
) {
}

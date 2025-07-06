package com.mc_host.api.model.user;

public record ApplicationUser(
    String clerkId,
    String customerId,
	Long pterodactylUserId,
	String pterodactylUsername,
	String pterodactylPassword,
	String primaryEmail,
	String dummyEmail
) {
}

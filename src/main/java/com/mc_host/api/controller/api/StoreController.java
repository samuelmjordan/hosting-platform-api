package com.mc_host.api.controller.api;

import com.mc_host.api.auth.CurrentUser;
import com.mc_host.api.model.stripe.request.CheckoutRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/user")
public interface StoreController {

	@PostMapping("checkout")
	public ResponseEntity<String> startCheckout(
		@CurrentUser String clerkId,
		@RequestBody CheckoutRequest request
	);
}

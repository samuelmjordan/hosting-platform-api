package com.mc_host.api.auth;

import com.mc_host.api.repository.SubscriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ValidatedSubscriptionResolver extends AbstractAuthResolver<ValidatedSubscription> {

	private final SubscriptionRepository subscriptionRepository;

	public ValidatedSubscriptionResolver(
		SubscriptionRepository subscriptionRepository
	) {
		super(ValidatedSubscription.class);
		this.subscriptionRepository = subscriptionRepository;
	}

	@Override
	protected String doResolve(NativeWebRequest webRequest) {
		String userId = getCurrentUserId();
		String subscriptionId = getPathVariable(webRequest, "subscriptionId");

		String subscriptionUserId = subscriptionRepository.selectSubscriptionOwnerUserId(subscriptionId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
				"Subscription %s not found".formatted(subscriptionId)));

		validateOwnership(subscriptionUserId, userId, subscriptionId, "subscription");
		return subscriptionId;
	}
}
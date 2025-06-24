package com.mc_host.api.auth;

import com.mc_host.api.model.subscription.ContentSubscription;
import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.repository.UserRepository;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ValidatedSubscriptionResolver extends AbstractAuthResolver<ValidatedSubscription> {

	private final SubscriptionRepository subscriptionRepository;
	private final UserRepository userRepository;

	public ValidatedSubscriptionResolver(
		SubscriptionRepository subscriptionRepository,
		UserRepository userRepository
	) {
		super(ValidatedSubscription.class);
		this.subscriptionRepository = subscriptionRepository;
		this.userRepository = userRepository;
	}

	@Override
	protected String doResolve(MethodParameter parameter, NativeWebRequest webRequest) {
		String userId = getCurrentUserId();
		String subscriptionId = getPathVariable(webRequest, "subscriptionId");

		String subscriptionCustomerId = subscriptionRepository.selectSubscription(subscriptionId)
			.map(ContentSubscription::customerId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
				"Subscription %s not found".formatted(subscriptionId)));

		String userCustomerId = userRepository.selectCustomerIdByClerkId(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
				"User %s not found".formatted(userId)));

		validateOwnership(userCustomerId, subscriptionCustomerId, userId, subscriptionId, "subscription");
		return subscriptionId;
	}
}
package com.mc_host.api.auth;

import com.mc_host.api.model.subscription.ContentSubscription;
import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ValidatedSubscriptionResolver implements HandlerMethodArgumentResolver {

	private final SubscriptionRepository subscriptionRepository;
	private final UserRepository userRepository;

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(ValidatedSubscription.class)
			&& parameter.getParameterType().equals(String.class);
	}

	@Override
	public String resolveArgument(
		MethodParameter parameter,
		ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest,
		WebDataBinderFactory binderFactory
	) {
		String userId = getCurrentUserId();
		String subscriptionId = getPathVariable(webRequest, "subscriptionId");

		String subscriptionCustomerId = subscriptionRepository.selectSubscription(subscriptionId)
			.map(ContentSubscription::customerId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription %s not found".formatted(subscriptionId)));
		String userCustomerId = userRepository.selectCustomerIdByClerkId(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User %s not found".formatted(userId)));

		if (!subscriptionCustomerId.equals(userCustomerId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User %s not authorised for subscription %s".formatted(userId, subscriptionId));
		}

		return subscriptionId;
	}

	private String getCurrentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth.getPrincipal() instanceof Jwt jwt) {
			return jwt.getSubject();
		}
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no authenticated user");
	}

	private String getPathVariable(NativeWebRequest request, String variableName) {
		Map<String, String> pathVariables = (Map<String, String>)
			request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

		return pathVariables != null ? pathVariables.get(variableName) : null;
	}
}

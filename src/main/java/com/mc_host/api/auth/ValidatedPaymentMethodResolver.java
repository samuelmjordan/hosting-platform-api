package com.mc_host.api.auth;

import com.mc_host.api.model.stripe.CustomerPaymentMethod;
import com.mc_host.api.repository.PaymentMethodRepository;
import com.mc_host.api.repository.UserRepository;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ValidatedPaymentMethodResolver extends AbstractAuthResolver<ValidatedPaymentMethod, CustomerPaymentMethod> {

	private final PaymentMethodRepository paymentMethodRepository;
	private final UserRepository userRepository;

	public ValidatedPaymentMethodResolver(
		PaymentMethodRepository paymentMethodRepository,
		UserRepository userRepository
	) {
		super(ValidatedPaymentMethod.class, CustomerPaymentMethod.class);
		this.paymentMethodRepository = paymentMethodRepository;
		this.userRepository = userRepository;
	}

	@Override
	protected String doResolveId(
		MethodParameter parameter,
		NativeWebRequest webRequest
	) {
		String userId = getCurrentUserId();
		String paymentMethodId = getPathVariable(webRequest, "paymentMethodId");

		String subscriptionCustomerId = paymentMethodRepository.selectPaymentMethod(paymentMethodId)
			.map(CustomerPaymentMethod::customerId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
				"Payment method %s not found".formatted(paymentMethodId)));

		String userCustomerId = userRepository.selectCustomerIdByClerkId(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
				"User %s not found".formatted(userId)));

		validateOwnership(userCustomerId, subscriptionCustomerId, userId, paymentMethodId, "payment method");
		return paymentMethodId;
	}

	@Override
	protected CustomerPaymentMethod doResolveEntity(
		MethodParameter parameter,
		NativeWebRequest webRequest
	) {
		String userId = getCurrentUserId();
		String paymentMethodId = getPathVariable(webRequest, "paymentMethodId");

		CustomerPaymentMethod paymentMethod = paymentMethodRepository.selectPaymentMethod(paymentMethodId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
				"Payment method %s not found".formatted(paymentMethodId)));

		String userCustomerId = userRepository.selectCustomerIdByClerkId(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
				"User %s not found".formatted(userId)));

		validateOwnership(userCustomerId, paymentMethod.customerId(), userId, paymentMethodId, "payment method");
		return paymentMethod;
	}
}
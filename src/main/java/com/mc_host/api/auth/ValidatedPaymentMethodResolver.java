package com.mc_host.api.auth;

import com.mc_host.api.repository.PaymentMethodRepository;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ValidatedPaymentMethodResolver extends AbstractAuthResolver<ValidatedPaymentMethod> {

	private final PaymentMethodRepository paymentMethodRepository;

	public ValidatedPaymentMethodResolver(
		PaymentMethodRepository paymentMethodRepository
	) {
		super(ValidatedPaymentMethod.class);
		this.paymentMethodRepository = paymentMethodRepository;
	}

	@Override
	protected String doResolve(MethodParameter parameter, NativeWebRequest webRequest) {
		String userId = getCurrentUserId();
		String paymentMethodId = getPathVariable(webRequest, "paymentMethodId");

		String paymentMethodOwner = paymentMethodRepository.selectPaymentMethodClerkId(paymentMethodId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
				"Payment method %s not found".formatted(paymentMethodId)));

		validateOwnership(paymentMethodOwner, userId, paymentMethodId, "payment method");
		return paymentMethodId;
	}
}

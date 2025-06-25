package com.mc_host.api.controller.api;

import com.mc_host.api.auth.CurrentUser;
import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.stripe.CustomerInvoice;
import com.mc_host.api.model.stripe.response.PaymentMethodResponse;
import com.mc_host.api.model.stripe.response.ServerSubscriptionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("api")
public interface UserController {

    @GetMapping("user/currency")
    public ResponseEntity<AcceptedCurrency> getUserCurrency(
        @CurrentUser String userId
    );

    @GetMapping("user/invoice")
    public ResponseEntity<List<CustomerInvoice>> getUserInvoices(
        @CurrentUser String userId
    );

    @GetMapping("user/payment-method")
    public ResponseEntity<List<PaymentMethodResponse>> getUserPaymentMethods(
        @CurrentUser String userId
    );

    @GetMapping("user/subscription")
    public ResponseEntity<List<ServerSubscriptionResponse>> getUserServerSubscriptions(
        @CurrentUser String userId
    );
}

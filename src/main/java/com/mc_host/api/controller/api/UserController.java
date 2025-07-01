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
@RequestMapping("api/user")
public interface UserController {

    @GetMapping("currency")
    public ResponseEntity<AcceptedCurrency> getUserCurrency(
        @CurrentUser String userId
    );

    @GetMapping("invoice")
    public ResponseEntity<List<CustomerInvoice>> getUserInvoices(
        @CurrentUser String userId
    );

    @GetMapping("payment-method")
    public ResponseEntity<List<PaymentMethodResponse>> getUserPaymentMethods(
        @CurrentUser String userId
    );

    @GetMapping("subscription")
    public ResponseEntity<List<ServerSubscriptionResponse>> getUserServerSubscriptions(
        @CurrentUser String userId
    );
}

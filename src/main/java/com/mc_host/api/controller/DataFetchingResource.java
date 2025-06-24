package com.mc_host.api.controller;

import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.plan.Plan;
import com.mc_host.api.model.plan.SpecificationType;
import com.mc_host.api.model.stripe.CustomerInvoice;
import com.mc_host.api.model.stripe.response.PaymentMethodResponse;
import com.mc_host.api.model.stripe.response.ServerSubscriptionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api")
public interface DataFetchingResource {

    @GetMapping("/user/{userId}/currency")
    public ResponseEntity<AcceptedCurrency> getUserCurrency(
        @PathVariable String userId
    );

    @GetMapping("/user/{userId}/invoice")
    public ResponseEntity<List<CustomerInvoice>> getUserInvoices(
        @PathVariable String userId
    );

    @GetMapping("/user/{userId}/payment-method")
    public ResponseEntity<List<PaymentMethodResponse>> getUserPaymentMethods(
        @PathVariable String userId
    );

    @GetMapping("/user/{userId}/subscription/server")
    public ResponseEntity<List<ServerSubscriptionResponse>> getUserServerSubscriptions(
        @PathVariable String userId
    );

    @GetMapping("/plan/{specType}")
    public ResponseEntity<List<Plan>> getPlansForSpecType(
        @PathVariable SpecificationType specType
    );
}

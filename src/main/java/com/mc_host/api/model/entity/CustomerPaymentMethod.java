package com.mc_host.api.model.entity;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.JsonNode;
import com.mc_host.api.model.PaymentMethodType;

public record CustomerPaymentMethod(
   String paymentMethodId,
   String customerId,
   PaymentMethodType paymentMethodType,
   String displayName,
   JsonNode paymentData,
   Boolean isActive,
   Boolean isDefault
) {
   
   public static CustomerPaymentMethod create(String paymentMethodId, String customerId, PaymentMethodType type, String displayName, JsonNode paymentData) {
       return new CustomerPaymentMethod(
           paymentMethodId,
           customerId, 
           type,
           displayName,
           paymentData,
           true,
           false
       );
   }
   
   public boolean isExpired() {
       if (paymentMethodType != PaymentMethodType.CARD) return false;
       
       var expMonth = paymentData.path("exp_month").asInt();
       var expYear = paymentData.path("exp_year").asInt();
       var now = LocalDate.now();
       
       return now.isAfter(LocalDate.of(expYear, expMonth, 1).plusMonths(1).minusDays(1));
   }

   public CustomerPaymentMethod setDefault() {
       return new CustomerPaymentMethod(
           this.paymentMethodId,
           this.customerId, 
           this.paymentMethodType,
           this.displayName,
           this.paymentData,
           this.isActive,
           true
       );
   }
}

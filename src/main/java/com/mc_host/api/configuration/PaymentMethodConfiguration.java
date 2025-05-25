package com.mc_host.api.configuration;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.stripe.param.PaymentLinkUpdateParams.PaymentMethodType;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "payment.method")
public class PaymentMethodConfiguration {
   
   private Map<String, Map<String, FieldConfig>> display;
   
   @Data
   public static class FieldConfig {
       private String label;
       private String displayType; // brand_icon, masked, text, wallet_icon, country_flag
   }
   
   public Map<String, FieldConfig> getFieldsForType(PaymentMethodType paymentType) {
       return display.getOrDefault(paymentType.name().toLowerCase(), Map.of());
   }
   
   public FieldConfig getFieldConfig(PaymentMethodType paymentType, String fieldName) {
       return display.getOrDefault(paymentType.name().toLowerCase(), Map.of())
            .get(fieldName);
   }
   
   public boolean hasField(PaymentMethodType paymentType, String fieldName) {
       return display.containsKey(paymentType.name().toLowerCase()) && 
              display.get(paymentType.name().toLowerCase()).containsKey(fieldName);
   }
}
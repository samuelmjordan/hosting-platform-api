package com.mc_host.api.service.stripe.events;

import java.util.List;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.model.PaymentMethodType;
import com.mc_host.api.model.cache.StripeEventType;
import com.mc_host.api.model.entity.CustomerPaymentMethod;
import com.mc_host.api.repository.PaymentMethodRepository;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentMethodListParams;

@Service
public class StripePaymentMethodService implements StripeEventService {
    private static final Logger LOGGER = Logger.getLogger(StripePaymentMethodService.class.getName());

    private final PaymentMethodRepository paymentMethodRepository;
    private final ObjectMapper objectMapper;

    public StripePaymentMethodService(
       PaymentMethodRepository paymentMethodRepository,
       ObjectMapper objectMapper
    ) {
       this.paymentMethodRepository = paymentMethodRepository;
       this.objectMapper = objectMapper;
    }

    public StripeEventType getType() {
        return StripeEventType.PAYMENT_METHOD;
    }

    public void process(String customerId) {
        try {
            LOGGER.info("Syncing payment method data for customer: " + customerId);

            PaymentMethodListParams params = PaymentMethodListParams.builder()
                .setCustomer(customerId)
                .build();

            List<CustomerPaymentMethod> paymentMethods = 
                PaymentMethod.list(params).getData().stream()
                    .map(pm -> stripePaymentMethodToEntity(pm, customerId))
                    .toList();

            paymentMethods.forEach(paymentMethodRepository::upsertPaymentMethod);
            
        } catch (Exception e) {
            LOGGER.severe("Error syncing payment method data for customer " + customerId + ": " + e.getMessage());
            throw new RuntimeException("Failed to sync payment method data", e);
        }
    }

    private CustomerPaymentMethod stripePaymentMethodToEntity(PaymentMethod stripePm, String customerId) {
        
        var type = determinePaymentMethodType(stripePm);
        var paymentData = createPaymentData(stripePm, type);
        var displayName = createDisplayName(stripePm, type);
        
        return CustomerPaymentMethod.create(
            stripePm.getId(),
            customerId,
            type,
            displayName,
            paymentData
        );
    }

    private PaymentMethodType determinePaymentMethodType(PaymentMethod stripePm) {
        return switch (stripePm.getType()) {
            case "card" -> {
                var wallet = stripePm.getCard().getWallet();
                if (wallet != null) {
                    yield switch (wallet.getType()) {
                        case "apple_pay" -> PaymentMethodType.APPLE_PAY;
                        case "google_pay" -> PaymentMethodType.GOOGLE_PAY;
                        case "samsung_pay" -> PaymentMethodType.SAMSUNG_PAY;
                        default -> PaymentMethodType.CARD;
                    };
                }
                yield PaymentMethodType.CARD;
            }
            case "sepa_debit" -> PaymentMethodType.SEPA;
            default -> throw new IllegalArgumentException("Unsupported payment method type: " + stripePm.getType());
        };
    }

    private JsonNode createPaymentData(PaymentMethod stripePm, PaymentMethodType type) {
        try {
            var data = objectMapper.createObjectNode();
            data.put("stripe_pm_id", stripePm.getId());
            
            return switch (type) {
                case CARD -> {
                    var card = stripePm.getCard();
                    data.put("brand", card.getBrand());
                    data.put("last_four", card.getLast4());
                    data.put("exp_month", card.getExpMonth().intValue());
                    data.put("exp_year", card.getExpYear().intValue());
                    data.put("funding", card.getFunding());
                    yield data;
                }
                case APPLE_PAY, GOOGLE_PAY, SAMSUNG_PAY -> {
                    var card = stripePm.getCard();
                    data.put("wallet_type", type.name().toLowerCase());
                    data.put("card_brand", card.getBrand());
                    data.put("card_last_four", card.getLast4());
                    yield data;
                }
                case SEPA -> {
                    var sepa = stripePm.getSepaDebit();
                    data.put("bank_name", sepa.getBankCode()); // might need mapping
                    data.put("last_four", sepa.getLast4());
                    data.put("country", sepa.getCountry());
                    yield data;
                }
            };
        } catch (Exception e) {
            throw new RuntimeException("Failed to create payment data", e);
        }
    }

    private String createDisplayName(PaymentMethod stripePm, PaymentMethodType type) {
        return switch (type) {
            case CARD -> {
                var card = stripePm.getCard();
                yield String.format("%s ending in %s", 
                    capitalizeFirst(card.getBrand()), card.getLast4());
            }
            case APPLE_PAY -> {
                var card = stripePm.getCard();
                yield String.format("Apple Pay (%s %s)", 
                    capitalizeFirst(card.getBrand()), card.getLast4());
            }
            case GOOGLE_PAY -> {
                var card = stripePm.getCard();
                yield String.format("Google Pay (%s %s)", 
                    capitalizeFirst(card.getBrand()), card.getLast4());
            }
            case SAMSUNG_PAY -> {
                var card = stripePm.getCard();
                yield String.format("Samsung Pay (%s %s)", 
                    capitalizeFirst(card.getBrand()), card.getLast4());
            }
            case SEPA -> {
                var sepa = stripePm.getSepaDebit();
                yield String.format("Bank transfer ending in %s", sepa.getLast4());
            }
        };
    }

    private String capitalizeFirst(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}

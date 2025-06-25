package com.mc_host.api.service.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.mc_host.api.configuration.PaymentMethodConfiguration;
import com.mc_host.api.configuration.PaymentMethodConfiguration.FieldConfig;
import com.mc_host.api.controller.api.DataFetchingController;
import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.plan.ContentPrice;
import com.mc_host.api.model.plan.ServerSpecification;
import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.resource.dns.DnsCNameRecord;
import com.mc_host.api.model.stripe.CustomerInvoice;
import com.mc_host.api.model.stripe.CustomerPaymentMethod;
import com.mc_host.api.model.stripe.SubscriptionStatus;
import com.mc_host.api.model.stripe.response.PaymentMethodResponse;
import com.mc_host.api.model.stripe.response.PaymentMethodResponse.DisplayField;
import com.mc_host.api.model.stripe.response.ServerSubscriptionResponse;
import com.mc_host.api.model.subscription.ContentSubscription;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.GameServerSpecRepository;
import com.mc_host.api.repository.InvoiceRepository;
import com.mc_host.api.repository.PaymentMethodRepository;
import com.mc_host.api.repository.PlanRepository;
import com.mc_host.api.repository.PriceRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class DataFetchingService implements DataFetchingController {
    private static final Logger LOGGER = Logger.getLogger(DataFetchingService.class.getName());

    private final PaymentMethodConfiguration paymentMethodConfiguration;
    private final PriceRepository priceRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final GameServerRepository gameServerRepository;
    private final GameServerSpecRepository gameServerSpecRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final ServerExecutionContextRepository serverExecutionContextRepository;

    @Override
    public ResponseEntity<AcceptedCurrency> getUserCurrency(String userId) {
        return ResponseEntity.ok(getUserCurrencyInner(userId));
    }

    public AcceptedCurrency getUserCurrencyInner(String userId) {
        return userRepository.selectUserCurrency(userId).orElse(AcceptedCurrency.XXX);
    }

    @Override
    public ResponseEntity<List<CustomerInvoice>> getUserInvoices(String userId) {
        LOGGER.log(Level.INFO, String.format("Fetching invoices for clerkId %s", userId));
        String customerId = getUserCustomerId(userId)
            .orElseThrow(() -> new IllegalStateException("User does not have a customer ID"));
        List<CustomerInvoice> invoices = invoiceRepository.selectInvoicesByCustomerId(customerId);
        return ResponseEntity.ok(invoices);
    }

    @Override
    public ResponseEntity<List<PaymentMethodResponse>> getUserPaymentMethods(String userId) {
        LOGGER.log(Level.INFO, String.format("Fetching payment methods for clerkId %s", userId));
        String customerId = getUserCustomerId(userId)
            .orElseThrow(() -> new IllegalStateException("User does not have a customer ID"));
        
        List<CustomerPaymentMethod> paymentMethods = paymentMethodRepository.selectPaymentMethodsByCustomerId(customerId);
        
        List<PaymentMethodResponse> dtos = paymentMethods.stream()
            .map(this::toPaymentMethodDto)
            .toList();
        
        return ResponseEntity.ok(dtos);
    }

    private PaymentMethodResponse toPaymentMethodDto(CustomerPaymentMethod paymentMethod) {
        var displayFields = getDisplayFields(paymentMethod);
        
        return new PaymentMethodResponse(
            paymentMethod.paymentMethodId(),
            paymentMethod.paymentMethodType().name().toLowerCase(),
            paymentMethod.displayName(),
            paymentMethod.isDefault(),
            paymentMethod.isActive(),
            displayFields
        );
    }

    public Map<String, DisplayField> getDisplayFields(CustomerPaymentMethod paymentMethod) {
        var typeConfig = paymentMethodConfiguration.getFieldsForType(paymentMethod.paymentMethodType());
        
        return typeConfig.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(
                Comparator.comparing(FieldConfig::getOrder)
            ))
            .collect(LinkedHashMap::new, (map, entry) -> {
                var fieldName = entry.getKey();
                var fieldConfig = entry.getValue();
                var value = extractValue(paymentMethod.paymentData(), fieldName);
                if (value != null) {
                    map.put(fieldName, new DisplayField(
                        value, 
                        fieldConfig.getLabel(), 
                        fieldConfig.getDisplayType()
                    ));
                }
            }, LinkedHashMap::putAll);
    }
    
    private String extractValue(JsonNode data, String fieldName) {
        return switch (fieldName) {
            case "brand" -> data.path("brand").asText();
            case "last_four" -> data.path("last_four").asText();
            case "exp_display" -> formatExpiry(data);
            case "card_brand" -> data.path("card_brand").asText();
            case "card_last_four" -> data.path("card_last_four").asText();
            case "wallet_type" -> data.path("wallet_type").asText();
            case "bank_name" -> data.path("bank_name").asText();
            case "country" -> data.path("country").asText();
            default -> null;
        };
    }
    
    private String formatExpiry(JsonNode data) {
        var month = data.path("exp_month").asInt();
        var year = data.path("exp_year").asInt();
        return String.format("%02d/%d", month, year % 100);
    }

    @Override
    public ResponseEntity<List<ServerSubscriptionResponse>> getUserServerSubscriptions(String userId) {
        LOGGER.log(Level.INFO, String.format("Fetching server subscriptions for clerkId %s", userId));

        Optional<String> customerId = getUserCustomerId(userId);
        if (!customerId.isPresent()) {
            return ResponseEntity.ok(List.of());
        }
        
        List<ServerSubscriptionResponse> serverSubscriptions = subscriptionRepository.selectSubscriptionsByCustomerId(customerId.get()).stream()
            .map(this::getServerSubscriptionResponse)
            .filter(subscription -> !SubscriptionStatus.fromString(subscription.subscriptionStatus()).isTerminated())
            .toList();
        return ResponseEntity.ok(serverSubscriptions);
    }

    public Optional<String> getUserCustomerId(String userId) {
        return userRepository.selectCustomerIdByClerkId(userId);
    }

    private ServerSubscriptionResponse getServerSubscriptionResponse(ContentSubscription subscription) {
        ContentPrice price = priceRepository.selectPrice(subscription.priceId())
            .orElseThrow(() -> new IllegalStateException("Couldnt find price " + subscription.priceId()));
        String specificationId = planRepository.selectSpecificationId(subscription.priceId())
            .orElseThrow(() -> new IllegalStateException("Couldnt find specification for price " + subscription.priceId()));
        ServerSpecification gameSeverSpecification = gameServerSpecRepository.selectSpecification(specificationId)
            .orElseThrow(() -> new IllegalStateException("Couldnt find specification for price " + subscription.priceId()));
        String dnsCNameRecordName = gameServerRepository.selectDnsCNameRecordWithSubscriptionId(subscription.subscriptionId())
            .map(DnsCNameRecord::recordName)
            .orElse(null);
        Context context = serverExecutionContextRepository.selectSubscription(subscription.subscriptionId())
            .orElseThrow(() -> new IllegalStateException("No existing context for subscription %s " + subscription.subscriptionId()));

        return new ServerSubscriptionResponse(
            subscription.subscriptionId(),
            context.getTitle(),
            gameSeverSpecification.title(),
            gameSeverSpecification.ram_gb().toString(),
            gameSeverSpecification.vcpu().toString(),
            gameSeverSpecification.ssd_gb().toString(),
            dnsCNameRecordName,
            subscription.status().toString(),
            subscription.currentPeriodEnd(),
            subscription.currentPeriodStart(),
            subscription.cancelAtPeriodEnd(),
            price.currency(),
            price.minorAmount()
        );
    }
    
}

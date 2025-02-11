package com.mc_host.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mc_host.api.model.Currency;
import com.mc_host.api.model.entity.PriceEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/api")
public interface DataFetchingResource {

    @GetMapping("/product/{productId}/prices")
    public ResponseEntity<List<PriceEntity>> getProductPrices(
        @PathVariable String productId
    );

    @GetMapping("/user/{userId}/currency")
    public ResponseEntity<Currency> getCurrency(
        @PathVariable String userId
    );
}

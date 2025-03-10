package com.mc_host.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mc_host.api.model.AcceptedCurrency;
import com.mc_host.api.model.Plan;
import com.mc_host.api.model.entity.ContentSubscription;
import com.mc_host.api.model.game_server.GameServer;
import com.mc_host.api.model.specification.SpecificationType;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/api")
public interface DataFetchingResource {
    @GetMapping("/user/{userId}/currency")
    public ResponseEntity<AcceptedCurrency> getUserCurrency(
        @PathVariable String userId
    );

    @GetMapping("/user/{userId}/subscription")
    public ResponseEntity<List<ContentSubscription>> getUserSubscriptions(
        @PathVariable String userId
    );

    @GetMapping("/user/{userId}/subscription/server")
    public ResponseEntity<List<GameServer>> getUserServers(
        @PathVariable String userId
    );

    @GetMapping("/user/{userId}/subscription/server/plan")
    public ResponseEntity<List<Plan>> getUserPlans(
        @PathVariable String userId
    );

    @GetMapping("/plan/{specType}")
    public ResponseEntity<List<Plan>> getPlansForSpecType(
        @PathVariable SpecificationType specType
    );
}

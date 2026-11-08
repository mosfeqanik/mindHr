package com.mindhr.controller;

import com.mindhr.dto.CurrentPlanResponse;
import com.mindhr.dto.SubscriptionHistoryResponse;
import com.mindhr.dto.SubscriptionPlanChangeRequest;
import com.mindhr.dto.SubscriptionRequest;
import com.mindhr.dto.SubscriptionResponse;
import com.mindhr.dto.SubscriptionStatusChangeRequest;
import com.mindhr.dto.SubscriptionUpdateRequest;
import com.mindhr.security.TenantContext;
import com.mindhr.service.PlanLimitService;
import com.mindhr.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final PlanLimitService planLimitService;

    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId) {
        return ResponseEntity.ok(subscriptionService.list(organizationId));
    }

    /** The plan the organization can use now, with its features (org users: their own organization). */
    @GetMapping("/current")
    public ResponseEntity<CurrentPlanResponse> current(
            @RequestParam(name = "organizationId", required = false) Long organizationId) {
        return ResponseEntity.ok(planLimitService.currentPlanInfo(TenantContext.resolveOrganizationId(organizationId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.getById(id));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<SubscriptionHistoryResponse>> history(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.history(id));
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(@Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.status(201).body(subscriptionService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> update(@PathVariable Long id, @Valid @RequestBody SubscriptionUpdateRequest request) {
        return ResponseEntity.ok(subscriptionService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SubscriptionResponse> changeStatus(
            @PathVariable Long id, @Valid @RequestBody SubscriptionStatusChangeRequest request) {
        return ResponseEntity.ok(subscriptionService.changeStatus(id, request));
    }

    @PatchMapping("/{id}/plan")
    public ResponseEntity<SubscriptionResponse> changePlan(
            @PathVariable Long id, @Valid @RequestBody SubscriptionPlanChangeRequest request) {
        return ResponseEntity.ok(subscriptionService.changePlan(id, request));
    }
}

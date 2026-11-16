package com.mindhr.controller;

import com.mindhr.dto.SubscriptionPlanRequest;
import com.mindhr.dto.SubscriptionPlanResponse;
import com.mindhr.service.SubscriptionPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subscription-plans")
@RequiredArgsConstructor
public class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;

    @GetMapping
    public ResponseEntity<List<SubscriptionPlanResponse>> list(
            @RequestParam(name = "activeOnly", defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(subscriptionPlanService.listAll(activeOnly));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionPlanResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionPlanService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SubscriptionPlanResponse> create(@Valid @RequestBody SubscriptionPlanRequest request) {
        return ResponseEntity.status(201).body(subscriptionPlanService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SubscriptionPlanResponse> update(
            @PathVariable Long id, @Valid @RequestBody SubscriptionPlanRequest request) {
        return ResponseEntity.ok(subscriptionPlanService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subscriptionPlanService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

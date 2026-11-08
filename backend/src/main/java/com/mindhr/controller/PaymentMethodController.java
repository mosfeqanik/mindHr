package com.mindhr.controller;

import com.mindhr.dto.PaymentMethodRequest;
import com.mindhr.dto.PaymentMethodResponse;
import com.mindhr.service.PaymentMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

/**
 * Org-scoped payment methods. An {@code ORG_USER} always operates on their own organization;
 * {@code organizationId} is only honored for platform staff (Finance/Support), who must supply
 * it explicitly. See {@link com.mindhr.security.TenantContext}.
 */
@RestController
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping
    public ResponseEntity<List<PaymentMethodResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId) {
        return ResponseEntity.ok(paymentMethodService.list(organizationId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentMethodResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(paymentMethodService.getById(id));
    }

    @PostMapping
    public ResponseEntity<PaymentMethodResponse> create(@Valid @RequestBody PaymentMethodRequest request) {
        return ResponseEntity.status(201).body(paymentMethodService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentMethodResponse> update(@PathVariable Long id, @Valid @RequestBody PaymentMethodRequest request) {
        return ResponseEntity.ok(paymentMethodService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paymentMethodService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

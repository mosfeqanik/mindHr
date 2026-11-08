package com.mindhr.controller;

import com.mindhr.dto.PaymentRequest;
import com.mindhr.dto.PaymentResponse;
import com.mindhr.dto.PaymentStatusChangeRequest;
import com.mindhr.dto.ReceiptResponse;
import com.mindhr.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId) {
        return ResponseEntity.ok(paymentService.list(organizationId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(201).body(paymentService.create(request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PaymentResponse> changeStatus(
            @PathVariable Long id, @Valid @RequestBody PaymentStatusChangeRequest request) {
        return ResponseEntity.ok(paymentService.changeStatus(id, request));
    }

    @PostMapping("/{id}/receipt")
    public ResponseEntity<ReceiptResponse> issueReceipt(@PathVariable Long id) {
        return ResponseEntity.status(201).body(paymentService.issueReceipt(id));
    }
}

package com.mindhr.controller;

import io.swagger.v3.oas.annotations.Operation;
import com.mindhr.dto.InvoiceRequest;
import com.mindhr.dto.InvoiceResponse;
import com.mindhr.dto.InvoiceStatusChangeRequest;
import com.mindhr.service.InvoiceService;
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
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @Operation(summary = "List invoices",
            description = "Org users: their own organization only. Platform staff: pass `organizationId`.")
    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId) {
        return ResponseEntity.ok(invoiceService.list(organizationId));
    }

    @Operation(summary = "Get an invoice",
            description = "Org users: their own organization only.")
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getById(id));
    }

    @Operation(summary = "Create an invoice",
            description = "Platform staff only (SUPER_ADMIN, SAAS_STAFF); organization users get 403. Starts as "
            + "PENDING.")
    @PostMapping
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.status(201).body(invoiceService.create(request));
    }

    @Operation(summary = "Update an invoice",
            description = "Platform staff only (SUPER_ADMIN, SAAS_STAFF); organization users get 403.")
    @PutMapping("/{id}")
    public ResponseEntity<InvoiceResponse> update(@PathVariable Long id, @Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.ok(invoiceService.update(id, request));
    }

    @Operation(summary = "Change invoice status",
            description = "Platform staff only (SUPER_ADMIN, SAAS_STAFF); organization users get 403. PAID sets "
            + "`paidAt`.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<InvoiceResponse> changeStatus(
            @PathVariable Long id, @Valid @RequestBody InvoiceStatusChangeRequest request) {
        return ResponseEntity.ok(invoiceService.changeStatus(id, request));
    }
}

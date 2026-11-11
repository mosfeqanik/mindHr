package com.mindhr.controller;

import io.swagger.v3.oas.annotations.Operation;
import com.mindhr.dto.ReceiptResponse;
import com.mindhr.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @Operation(summary = "List receipts",
            description = "Org users: their own organization only. Platform staff: pass `organizationId`. Receipts "
            + "are created with POST /api/payments/{id}/receipt.")
    @GetMapping
    public ResponseEntity<List<ReceiptResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId) {
        return ResponseEntity.ok(receiptService.list(organizationId));
    }

    @Operation(summary = "Get a receipt",
            description = "Org users: their own organization only.")
    @GetMapping("/{id}")
    public ResponseEntity<ReceiptResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(receiptService.getById(id));
    }
}

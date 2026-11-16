package com.mindhr.controller;

import com.mindhr.dto.*;
import com.mindhr.service.EmployeePaymentAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment-accounts")
@RequiredArgsConstructor
public class EmployeePaymentAccountController {

    private final EmployeePaymentAccountService service;

    @PreAuthorize("hasAuthority('PAYROLL_VIEW')")
    @GetMapping
    public ResponseEntity<List<EmployeePaymentAccountResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId,
            @RequestParam(name = "employeeId", required = false) Long employeeId) {
        return ResponseEntity.ok(service.list(organizationId, employeeId));
    }

    @PreAuthorize("hasAuthority('PAYROLL_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<EmployeePaymentAccountResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PreAuthorize("hasAuthority('PAYROLL_CREATE')")
    @PostMapping
    public ResponseEntity<EmployeePaymentAccountResponse> create(@Valid @RequestBody EmployeePaymentAccountRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PreAuthorize("hasAuthority('PAYROLL_UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<EmployeePaymentAccountResponse> update(@PathVariable Long id, @Valid @RequestBody EmployeePaymentAccountRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PreAuthorize("hasAuthority('PAYROLL_DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}

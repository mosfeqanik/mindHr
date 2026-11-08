package com.mindhr.controller;

import com.mindhr.dto.*;
import com.mindhr.service.SalaryStructureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salary-structures")
@RequiredArgsConstructor
public class SalaryStructureController {

    private final SalaryStructureService service;

    @PreAuthorize("hasAuthority('PAYROLL_VIEW')")
    @GetMapping
    public ResponseEntity<List<SalaryStructureResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId,
            @RequestParam(name = "employeeId", required = false) Long employeeId) {
        return ResponseEntity.ok(service.list(organizationId, employeeId));
    }

    @PreAuthorize("hasAuthority('PAYROLL_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<SalaryStructureResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PreAuthorize("hasAuthority('PAYROLL_CREATE')")
    @PostMapping
    public ResponseEntity<SalaryStructureResponse> create(@Valid @RequestBody SalaryStructureRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PreAuthorize("hasAuthority('PAYROLL_UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<SalaryStructureResponse> update(@PathVariable Long id, @Valid @RequestBody SalaryStructureRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PreAuthorize("hasAuthority('PAYROLL_DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}

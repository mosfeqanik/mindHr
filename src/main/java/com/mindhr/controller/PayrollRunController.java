package com.mindhr.controller;

import com.mindhr.dto.*;
import com.mindhr.service.PayrollRunService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll-runs")
@RequiredArgsConstructor
public class PayrollRunController {

    private final PayrollRunService service;

    @PreAuthorize("hasAuthority('PAYROLL_VIEW')")
    @GetMapping
    public ResponseEntity<List<PayrollRunResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId) {
        return ResponseEntity.ok(service.list(organizationId));
    }

    @PreAuthorize("hasAuthority('PAYROLL_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<PayrollRunResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

}

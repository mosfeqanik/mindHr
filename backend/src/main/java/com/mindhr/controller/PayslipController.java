package com.mindhr.controller;

import com.mindhr.dto.*;
import com.mindhr.service.PayslipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payslips")
@RequiredArgsConstructor
public class PayslipController {

    private final PayslipService service;

    @PreAuthorize("hasAuthority('PAYSLIP_VIEW')")
    @GetMapping
    public ResponseEntity<List<PayslipResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId,
            @RequestParam(name = "employeeId", required = false) Long employeeId) {
        return ResponseEntity.ok(service.list(organizationId, employeeId));
    }

    @PreAuthorize("hasAuthority('PAYSLIP_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<PayslipResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

}

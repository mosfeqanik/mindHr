package com.mindhr.controller;

import com.mindhr.dto.PayrollRunCreateRequest;
import com.mindhr.dto.PayrollRunResponse;
import com.mindhr.dto.PayslipAdjustRequest;
import com.mindhr.dto.PayslipResponse;
import com.mindhr.service.PayrollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PayrollActionController {

    private final PayrollService service;

    /** Generates DRAFT payslips for the month. */
    @PreAuthorize("hasAuthority('PAYROLL_CREATE')")
    @PostMapping("/payroll-runs")
    public ResponseEntity<PayrollRunResponse> generate(@Valid @RequestBody PayrollRunCreateRequest request) {
        return ResponseEntity.status(201).body(service.generate(request));
    }

    @PreAuthorize("hasAuthority('PAYROLL_APPROVE')")
    @PostMapping("/payroll-runs/{id}/finalize")
    public ResponseEntity<PayrollRunResponse> finalizeRun(@PathVariable Long id) {
        return ResponseEntity.ok(service.finalizeRun(id));
    }

    @PreAuthorize("hasAuthority('PAYROLL_APPROVE')")
    @PostMapping("/payroll-runs/{id}/pay")
    public ResponseEntity<PayrollRunResponse> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(service.markPaid(id));
    }

    @PreAuthorize("hasAuthority('PAYROLL_DELETE')")
    @DeleteMapping("/payroll-runs/{id}")
    public ResponseEntity<Void> deleteDraft(@PathVariable Long id) {
        service.deleteDraft(id);
        return ResponseEntity.noContent().build();
    }

    /** Overtime, bonus, commission, loan and advance on a DRAFT payslip. */
    @PreAuthorize("hasAuthority('PAYROLL_UPDATE')")
    @PutMapping("/payslips/{id}/adjustments")
    public ResponseEntity<PayslipResponse> adjust(@PathVariable Long id, @Valid @RequestBody PayslipAdjustRequest request) {
        return ResponseEntity.ok(service.adjust(id, request));
    }
}

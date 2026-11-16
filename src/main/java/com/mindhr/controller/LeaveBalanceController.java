package com.mindhr.controller;

import com.mindhr.dto.*;
import com.mindhr.service.LeaveBalanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave-balances")
@RequiredArgsConstructor
public class LeaveBalanceController {

    private final LeaveBalanceService service;

    @PreAuthorize("hasAuthority('LEAVE_VIEW')")
    @GetMapping
    public ResponseEntity<List<LeaveBalanceResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId,
            @RequestParam(name = "employeeId", required = false) Long employeeId) {
        return ResponseEntity.ok(service.list(organizationId, employeeId));
    }

    @PreAuthorize("hasAuthority('LEAVE_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<LeaveBalanceResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PreAuthorize("hasAuthority('LEAVE_MANAGE')")
    @PostMapping
    public ResponseEntity<LeaveBalanceResponse> create(@Valid @RequestBody LeaveBalanceRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PreAuthorize("hasAuthority('LEAVE_MANAGE')")
    @PutMapping("/{id}")
    public ResponseEntity<LeaveBalanceResponse> update(@PathVariable Long id, @Valid @RequestBody LeaveBalanceRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PreAuthorize("hasAuthority('LEAVE_MANAGE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}

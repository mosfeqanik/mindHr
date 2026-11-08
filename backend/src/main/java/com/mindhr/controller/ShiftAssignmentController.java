package com.mindhr.controller;

import com.mindhr.dto.*;
import com.mindhr.service.ShiftAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shift-assignments")
@RequiredArgsConstructor
public class ShiftAssignmentController {

    private final ShiftAssignmentService service;

    @PreAuthorize("hasAuthority('SHIFT_VIEW')")
    @GetMapping
    public ResponseEntity<List<ShiftAssignmentResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId,
            @RequestParam(name = "employeeId", required = false) Long employeeId) {
        return ResponseEntity.ok(service.list(organizationId, employeeId));
    }

    @PreAuthorize("hasAuthority('SHIFT_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<ShiftAssignmentResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PreAuthorize("hasAuthority('SHIFT_CREATE')")
    @PostMapping
    public ResponseEntity<ShiftAssignmentResponse> create(@Valid @RequestBody ShiftAssignmentRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PreAuthorize("hasAuthority('SHIFT_UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<ShiftAssignmentResponse> update(@PathVariable Long id, @Valid @RequestBody ShiftAssignmentRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PreAuthorize("hasAuthority('SHIFT_DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}

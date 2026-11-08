package com.mindhr.controller;

import com.mindhr.dto.*;
import com.mindhr.service.LeaveTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave-types")
@RequiredArgsConstructor
public class LeaveTypeController {

    private final LeaveTypeService service;

    @PreAuthorize("hasAuthority('LEAVE_VIEW')")
    @GetMapping
    public ResponseEntity<List<LeaveTypeResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId) {
        return ResponseEntity.ok(service.list(organizationId));
    }

    @PreAuthorize("hasAuthority('LEAVE_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<LeaveTypeResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PreAuthorize("hasAuthority('LEAVE_MANAGE')")
    @PostMapping
    public ResponseEntity<LeaveTypeResponse> create(@Valid @RequestBody LeaveTypeRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PreAuthorize("hasAuthority('LEAVE_MANAGE')")
    @PutMapping("/{id}")
    public ResponseEntity<LeaveTypeResponse> update(@PathVariable Long id, @Valid @RequestBody LeaveTypeRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PreAuthorize("hasAuthority('LEAVE_MANAGE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}

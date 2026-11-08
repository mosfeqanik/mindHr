package com.mindhr.controller;

import com.mindhr.dto.*;
import com.mindhr.service.EmployeeEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employee-events")
@RequiredArgsConstructor
public class EmployeeEventController {

    private final EmployeeEventService service;

    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW')")
    @GetMapping
    public ResponseEntity<List<EmployeeEventResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId,
            @RequestParam(name = "employeeId", required = false) Long employeeId) {
        return ResponseEntity.ok(service.list(organizationId, employeeId));
    }

    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeEventResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE')")
    @PostMapping
    public ResponseEntity<EmployeeEventResponse> create(@Valid @RequestBody EmployeeEventRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}

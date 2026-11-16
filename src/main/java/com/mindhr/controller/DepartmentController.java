package com.mindhr.controller;

import com.mindhr.dto.*;
import com.mindhr.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService service;

    @PreAuthorize("hasAuthority('DEPARTMENT_VIEW')")
    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId) {
        return ResponseEntity.ok(service.list(organizationId));
    }

    @PreAuthorize("hasAuthority('DEPARTMENT_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PreAuthorize("hasAuthority('DEPARTMENT_CREATE')")
    @PostMapping
    public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PreAuthorize("hasAuthority('DEPARTMENT_UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> update(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PreAuthorize("hasAuthority('DEPARTMENT_DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}

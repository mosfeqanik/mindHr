package com.mindhr.controller;

import com.mindhr.dto.*;
import com.mindhr.service.EmployeeDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class EmployeeDocumentController {

    private final EmployeeDocumentService service;

    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    @GetMapping
    public ResponseEntity<List<EmployeeDocumentResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId,
            @RequestParam(name = "employeeId", required = false) Long employeeId) {
        return ResponseEntity.ok(service.list(organizationId, employeeId));
    }

    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDocumentResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PreAuthorize("hasAuthority('DOCUMENT_CREATE')")
    @PostMapping
    public ResponseEntity<EmployeeDocumentResponse> create(@Valid @RequestBody EmployeeDocumentRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PreAuthorize("hasAuthority('DOCUMENT_UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeDocumentResponse> update(@PathVariable Long id, @Valid @RequestBody EmployeeDocumentRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PreAuthorize("hasAuthority('DOCUMENT_DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}

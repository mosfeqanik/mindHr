package com.mindhr.controller;

import com.mindhr.dto.*;
import com.mindhr.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService service;

    @PreAuthorize("hasAuthority('BRANCH_VIEW')")
    @GetMapping
    public ResponseEntity<List<BranchResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId) {
        return ResponseEntity.ok(service.list(organizationId));
    }

    @PreAuthorize("hasAuthority('BRANCH_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<BranchResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PreAuthorize("hasAuthority('BRANCH_CREATE')")
    @PostMapping
    public ResponseEntity<BranchResponse> create(@Valid @RequestBody BranchRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PreAuthorize("hasAuthority('BRANCH_UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<BranchResponse> update(@PathVariable Long id, @Valid @RequestBody BranchRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PreAuthorize("hasAuthority('BRANCH_DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}

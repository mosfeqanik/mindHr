package com.mindhr.controller;

import com.mindhr.dto.*;
import com.mindhr.service.ShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService service;

    @PreAuthorize("hasAuthority('SHIFT_VIEW')")
    @GetMapping
    public ResponseEntity<List<ShiftResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId) {
        return ResponseEntity.ok(service.list(organizationId));
    }

    @PreAuthorize("hasAuthority('SHIFT_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<ShiftResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PreAuthorize("hasAuthority('SHIFT_CREATE')")
    @PostMapping
    public ResponseEntity<ShiftResponse> create(@Valid @RequestBody ShiftRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PreAuthorize("hasAuthority('SHIFT_UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<ShiftResponse> update(@PathVariable Long id, @Valid @RequestBody ShiftRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PreAuthorize("hasAuthority('SHIFT_DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}

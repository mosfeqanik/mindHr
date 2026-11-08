package com.mindhr.controller;

import com.mindhr.dto.*;
import com.mindhr.service.AssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService service;

    @PreAuthorize("hasAuthority('ASSET_VIEW')")
    @GetMapping
    public ResponseEntity<List<AssetResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId) {
        return ResponseEntity.ok(service.list(organizationId));
    }

    @PreAuthorize("hasAuthority('ASSET_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<AssetResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PreAuthorize("hasAuthority('ASSET_CREATE')")
    @PostMapping
    public ResponseEntity<AssetResponse> create(@Valid @RequestBody AssetRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PreAuthorize("hasAuthority('ASSET_UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<AssetResponse> update(@PathVariable Long id, @Valid @RequestBody AssetRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PreAuthorize("hasAuthority('ASSET_DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}

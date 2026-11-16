package com.mindhr.controller;

import com.mindhr.dto.*;
import com.mindhr.service.AssetAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asset-assignments")
@RequiredArgsConstructor
public class AssetAssignmentController {

    private final AssetAssignmentService service;

    @PreAuthorize("hasAuthority('ASSET_VIEW')")
    @GetMapping
    public ResponseEntity<List<AssetAssignmentResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId,
            @RequestParam(name = "employeeId", required = false) Long employeeId) {
        return ResponseEntity.ok(service.list(organizationId, employeeId));
    }

    @PreAuthorize("hasAuthority('ASSET_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<AssetAssignmentResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

}

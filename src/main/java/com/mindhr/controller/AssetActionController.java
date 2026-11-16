package com.mindhr.controller;

import com.mindhr.dto.AssetAssignRequest;
import com.mindhr.dto.AssetAssignmentResponse;
import com.mindhr.dto.AssetReturnRequest;
import com.mindhr.service.AssetActionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/asset-assignments")
@RequiredArgsConstructor
public class AssetActionController {

    private final AssetActionService service;

    @PreAuthorize("hasAuthority('ASSET_CREATE')")
    @PostMapping
    public ResponseEntity<AssetAssignmentResponse> assign(@Valid @RequestBody AssetAssignRequest request) {
        return ResponseEntity.status(201).body(service.assign(request));
    }

    @PreAuthorize("hasAuthority('ASSET_UPDATE')")
    @PostMapping("/{id}/return")
    public ResponseEntity<AssetAssignmentResponse> returnAsset(@PathVariable Long id,
                                                               @Valid @RequestBody(required = false) AssetReturnRequest request) {
        return ResponseEntity.ok(service.returnAsset(id, request != null ? request : new AssetReturnRequest(null, null, null)));
    }
}

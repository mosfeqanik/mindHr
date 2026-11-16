package com.mindhr.controller;

import com.mindhr.dto.OrganizationSettingRequest;
import com.mindhr.dto.OrganizationSettingResponse;
import com.mindhr.service.OrganizationSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Org-scoped settings. An {@code ORG_USER} always operates on their own organization;
 * {@code organizationId} is only honored for platform staff, who must supply it explicitly.
 * See {@link com.mindhr.security.TenantContext}.
 */
@RestController
@RequestMapping("/api/organization-settings")
@RequiredArgsConstructor
public class OrganizationSettingController {

    private final OrganizationSettingService organizationSettingService;

    @GetMapping
    public ResponseEntity<List<OrganizationSettingResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId) {
        return ResponseEntity.ok(organizationSettingService.list(organizationId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationSettingResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(organizationSettingService.getById(id));
    }

    @GetMapping("/key/{key}")
    public ResponseEntity<OrganizationSettingResponse> getByKey(
            @PathVariable String key,
            @RequestParam(name = "organizationId", required = false) Long organizationId) {
        return ResponseEntity.ok(organizationSettingService.getByKey(key, organizationId));
    }

    @PostMapping
    public ResponseEntity<OrganizationSettingResponse> create(@Valid @RequestBody OrganizationSettingRequest request) {
        return ResponseEntity.status(201).body(organizationSettingService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrganizationSettingResponse> update(
            @PathVariable Long id, @Valid @RequestBody OrganizationSettingRequest request) {
        return ResponseEntity.ok(organizationSettingService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        organizationSettingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package com.mindhr.controller;

import io.swagger.v3.oas.annotations.Operation;
import com.mindhr.dto.RoleRequest;
import com.mindhr.dto.RoleResponse;
import com.mindhr.service.RoleService;
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

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "List roles",
            description = "Platform staff only (SUPER_ADMIN, SAAS_STAFF); organization users get 403. With "
            + "`organizationId`: that organization's roles plus platform roles; without: platform roles "
            + "only.")
    @GetMapping
    public ResponseEntity<List<RoleResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId) {
        return ResponseEntity.ok(roleService.list(organizationId));
    }

    @Operation(summary = "Get a role",
            description = "Platform staff only (SUPER_ADMIN, SAAS_STAFF); organization users get 403.")
    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getById(id));
    }

    @Operation(summary = "Create a role",
            description = "Platform staff only (SUPER_ADMIN, SAAS_STAFF); organization users get 403. With "
            + "`organizationId`: a custom role of that organization (data scope cannot be PLATFORM). "
            + "Without: a platform role (Super Admin only).")
    @PostMapping
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.status(201).body(roleService.create(request));
    }

    @Operation(summary = "Replace a role",
            description = "Platform staff only (SUPER_ADMIN, SAAS_STAFF); organization users get 403. `permissionIds` "
            + "replaces all permissions. System roles (SUPER_ADMIN and every organization's built-in "
            + "roles) cannot be changed.")
    @PutMapping("/{id}")
    public ResponseEntity<RoleResponse> update(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(roleService.update(id, request));
    }

    @Operation(summary = "Delete a role",
            description = "Platform staff only (SUPER_ADMIN, SAAS_STAFF); organization users get 403. System roles "
            + "cannot be deleted.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

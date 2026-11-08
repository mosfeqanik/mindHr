package com.mindhr.controller;

import io.swagger.v3.oas.annotations.Operation;
import com.mindhr.dto.UserRequest;
import com.mindhr.dto.UserResponse;
import com.mindhr.service.UserService;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "List users",
            description = "Platform staff only (SUPER_ADMIN, SAAS_STAFF); organization users get 403. Pass "
            + "`organizationId` to list one organization's users.")
    @GetMapping
    public ResponseEntity<List<UserResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId) {
        return ResponseEntity.ok(userService.list(organizationId));
    }

    @Operation(summary = "Get a user",
            description = "Platform staff: any user. Organization users: only their own record (403 for anyone else).")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @Operation(summary = "Create a user",
            description = "Platform staff only (SUPER_ADMIN, SAAS_STAFF); organization users get 403. SUPER_ADMIN / "
            + "SAAS_STAFF accounts (no `organizationId`) need the Super Admin. An account with an "
            + "`organizationId` must be `ORG_USER` (400 otherwise). `roleIds` must be roles of that "
            + "organization (platform roles for platform accounts).")
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(201).body(userService.create(request));
    }

    @Operation(summary = "Replace a user",
            description = "Platform staff only (SUPER_ADMIN, SAAS_STAFF); organization users get 403. Send every "
            + "field back (GET it first). `password` null keeps the current one; `roleIds` replaces all "
            + "roles and must belong to the user's organization. An account with an `organizationId` must "
            + "be `ORG_USER`. Platform accounts need the Super Admin.")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @Operation(summary = "Delete a user",
            description = "Platform staff only (SUPER_ADMIN, SAAS_STAFF); organization users get 403. You cannot "
            + "delete your own account; platform accounts need the Super Admin.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

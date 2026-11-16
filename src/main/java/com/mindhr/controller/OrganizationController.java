package com.mindhr.controller;

import com.mindhr.dto.OrganizationRequest;
import com.mindhr.dto.OrganizationResponse;
import com.mindhr.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    public ResponseEntity<List<OrganizationResponse>> list() {
        return ResponseEntity.ok(organizationService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.getById(id));
    }

    @PostMapping
    public ResponseEntity<OrganizationResponse> create(@Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.status(201).body(organizationService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrganizationResponse> update(@PathVariable Long id, @Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.ok(organizationService.update(id, request));
    }

    @PatchMapping("/{id}/owner")
    public ResponseEntity<OrganizationResponse> setOwner(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long ownerUserId = body.get("ownerUserId");
        if (ownerUserId == null) {
            throw new IllegalArgumentException("ownerUserId is required");
        }
        return ResponseEntity.ok(organizationService.setOwner(id, ownerUserId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        organizationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

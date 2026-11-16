package com.mindhr.controller;

import com.mindhr.dto.MenuImageRequest;
import com.mindhr.dto.MenuImageResponse;
import com.mindhr.service.MenuImageService;
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
 * Org-scoped menu gallery images. An {@code ORG_USER} always operates on their own organization;
 * {@code organizationId} is only honored for platform staff, who must supply it explicitly.
 * See {@link com.mindhr.security.TenantContext}.
 */
@RestController
@RequestMapping("/api/menu-images")
@RequiredArgsConstructor
public class MenuImageController {

    private final MenuImageService menuImageService;

    @GetMapping
    public ResponseEntity<List<MenuImageResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId) {
        return ResponseEntity.ok(menuImageService.list(organizationId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuImageResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(menuImageService.getById(id));
    }

    @PostMapping
    public ResponseEntity<MenuImageResponse> create(@Valid @RequestBody MenuImageRequest request) {
        return ResponseEntity.status(201).body(menuImageService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MenuImageResponse> update(@PathVariable Long id, @Valid @RequestBody MenuImageRequest request) {
        return ResponseEntity.ok(menuImageService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        menuImageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

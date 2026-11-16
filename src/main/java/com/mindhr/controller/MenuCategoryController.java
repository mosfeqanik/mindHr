package com.mindhr.controller;

import com.mindhr.dto.MenuCategoryRequest;
import com.mindhr.dto.MenuCategoryResponse;
import com.mindhr.service.MenuCategoryService;
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
@RequestMapping("/api/menu-categories")
@RequiredArgsConstructor
public class MenuCategoryController {

    private final MenuCategoryService menuCategoryService;

    @GetMapping
    public ResponseEntity<List<MenuCategoryResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId) {
        return ResponseEntity.ok(menuCategoryService.list(organizationId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuCategoryResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(menuCategoryService.getById(id));
    }

    @PostMapping
    public ResponseEntity<MenuCategoryResponse> create(@Valid @RequestBody MenuCategoryRequest request) {
        return ResponseEntity.status(201).body(menuCategoryService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MenuCategoryResponse> update(@PathVariable Long id, @Valid @RequestBody MenuCategoryRequest request) {
        return ResponseEntity.ok(menuCategoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        menuCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package com.mindhr.controller;

import com.mindhr.dto.MenuItemRequest;
import com.mindhr.dto.MenuItemResponse;
import com.mindhr.service.MenuItemService;
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
@RequestMapping("/api/menu-items")
@RequiredArgsConstructor
public class MenuItemController {

    private final MenuItemService menuItemService;

    @GetMapping
    public ResponseEntity<List<MenuItemResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId) {
        return ResponseEntity.ok(menuItemService.list(organizationId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuItemResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(menuItemService.getById(id));
    }

    @PostMapping
    public ResponseEntity<MenuItemResponse> create(@Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.status(201).body(menuItemService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MenuItemResponse> update(@PathVariable Long id, @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(menuItemService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        menuItemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

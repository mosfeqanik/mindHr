package com.mindhr.controller;

import com.mindhr.dto.SystemSettingRequest;
import com.mindhr.dto.SystemSettingResponse;
import com.mindhr.service.SystemSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system-settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    @GetMapping
    public ResponseEntity<List<SystemSettingResponse>> list() {
        return ResponseEntity.ok(systemSettingService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SystemSettingResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(systemSettingService.getById(id));
    }

    @GetMapping("/key/{key}")
    public ResponseEntity<SystemSettingResponse> getByKey(@PathVariable String key) {
        return ResponseEntity.ok(systemSettingService.getByKey(key));
    }

    @PostMapping
    public ResponseEntity<SystemSettingResponse> create(@Valid @RequestBody SystemSettingRequest request) {
        return ResponseEntity.status(201).body(systemSettingService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SystemSettingResponse> update(@PathVariable Long id, @Valid @RequestBody SystemSettingRequest request) {
        return ResponseEntity.ok(systemSettingService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        systemSettingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

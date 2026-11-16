package com.mindhr.controller;

import com.mindhr.dto.SalesActivityRequest;
import com.mindhr.dto.SalesActivityResponse;
import com.mindhr.service.SalesActivityService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales-activities")
@RequiredArgsConstructor
public class SalesActivityController {

    private final SalesActivityService salesActivityService;

    @GetMapping("/{id}")
    public ResponseEntity<SalesActivityResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(salesActivityService.getById(id));
    }

    @PostMapping
    public ResponseEntity<SalesActivityResponse> create(@Valid @RequestBody SalesActivityRequest request) {
        return ResponseEntity.status(201).body(salesActivityService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalesActivityResponse> update(@PathVariable Long id, @Valid @RequestBody SalesActivityRequest request) {
        return ResponseEntity.ok(salesActivityService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        salesActivityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

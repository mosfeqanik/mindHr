package com.mindhr.controller;

import com.mindhr.dto.LeadConvertRequest;
import com.mindhr.dto.LeadRequest;
import com.mindhr.dto.LeadResponse;
import com.mindhr.dto.SalesActivityResponse;
import com.mindhr.service.LeadService;
import com.mindhr.service.SalesActivityService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;
    private final SalesActivityService salesActivityService;

    @GetMapping
    public ResponseEntity<List<LeadResponse>> list() {
        return ResponseEntity.ok(leadService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeadResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(leadService.getById(id));
    }

    @GetMapping("/{id}/activities")
    public ResponseEntity<List<SalesActivityResponse>> activities(@PathVariable Long id) {
        return ResponseEntity.ok(salesActivityService.listByLead(id));
    }

    @PostMapping
    public ResponseEntity<LeadResponse> create(@Valid @RequestBody LeadRequest request) {
        return ResponseEntity.status(201).body(leadService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LeadResponse> update(@PathVariable Long id, @Valid @RequestBody LeadRequest request) {
        return ResponseEntity.ok(leadService.update(id, request));
    }

    @PatchMapping("/{id}/convert")
    public ResponseEntity<LeadResponse> convert(@PathVariable Long id, @Valid @RequestBody LeadConvertRequest request) {
        return ResponseEntity.ok(leadService.convert(id, request.organizationId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        leadService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

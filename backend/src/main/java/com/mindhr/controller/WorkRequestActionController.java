package com.mindhr.controller;

import com.mindhr.dto.DecisionRequest;
import com.mindhr.dto.WorkRequestCreateRequest;
import com.mindhr.dto.WorkRequestResponse;
import com.mindhr.service.WorkRequestActionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class WorkRequestActionController {

    private final WorkRequestActionService service;

    @PreAuthorize("hasAuthority('REQUEST_CREATE')")
    @PostMapping
    public ResponseEntity<WorkRequestResponse> create(@Valid @RequestBody WorkRequestCreateRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PreAuthorize("hasAuthority('REQUEST_APPROVE')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<WorkRequestResponse> approve(@PathVariable Long id,
                                                       @Valid @RequestBody(required = false) DecisionRequest request) {
        return ResponseEntity.ok(service.approve(id, request != null ? request.note() : null));
    }

    @PreAuthorize("hasAuthority('REQUEST_APPROVE')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<WorkRequestResponse> reject(@PathVariable Long id,
                                                      @Valid @RequestBody(required = false) DecisionRequest request) {
        return ResponseEntity.ok(service.reject(id, request != null ? request.note() : null));
    }

    @PreAuthorize("hasAuthority('REQUEST_CREATE')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<WorkRequestResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancel(id));
    }
}

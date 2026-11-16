package com.mindhr.controller;

import com.mindhr.dto.*;
import com.mindhr.service.WorkRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class WorkRequestController {

    private final WorkRequestService service;

    @PreAuthorize("hasAuthority('REQUEST_VIEW')")
    @GetMapping
    public ResponseEntity<List<WorkRequestResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId,
            @RequestParam(name = "employeeId", required = false) Long employeeId) {
        return ResponseEntity.ok(service.list(organizationId, employeeId));
    }

    @PreAuthorize("hasAuthority('REQUEST_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<WorkRequestResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

}

package com.mindhr.controller;

import com.mindhr.dto.*;
import com.mindhr.service.LeaveRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave-requests")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService service;

    @PreAuthorize("hasAuthority('LEAVE_VIEW')")
    @GetMapping
    public ResponseEntity<List<LeaveRequestResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId,
            @RequestParam(name = "employeeId", required = false) Long employeeId) {
        return ResponseEntity.ok(service.list(organizationId, employeeId));
    }

    @PreAuthorize("hasAuthority('LEAVE_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequestResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

}

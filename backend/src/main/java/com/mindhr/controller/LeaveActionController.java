package com.mindhr.controller;

import com.mindhr.dto.DecisionRequest;
import com.mindhr.dto.LeaveApplyRequest;
import com.mindhr.dto.LeaveRequestResponse;
import com.mindhr.service.LeaveActionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leave-requests")
@RequiredArgsConstructor
public class LeaveActionController {

    private final LeaveActionService service;

    @PreAuthorize("hasAuthority('LEAVE_CREATE')")
    @PostMapping
    public ResponseEntity<LeaveRequestResponse> apply(@Valid @RequestBody LeaveApplyRequest request) {
        return ResponseEntity.status(201).body(service.apply(request));
    }

    @PreAuthorize("hasAuthority('LEAVE_APPROVE')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<LeaveRequestResponse> approve(@PathVariable Long id,
                                                        @Valid @RequestBody(required = false) DecisionRequest request) {
        return ResponseEntity.ok(service.approve(id, request != null ? request.note() : null));
    }

    @PreAuthorize("hasAuthority('LEAVE_APPROVE')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<LeaveRequestResponse> reject(@PathVariable Long id,
                                                       @Valid @RequestBody(required = false) DecisionRequest request) {
        return ResponseEntity.ok(service.reject(id, request != null ? request.note() : null));
    }

    @PreAuthorize("hasAuthority('LEAVE_CREATE')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<LeaveRequestResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancel(id));
    }
}

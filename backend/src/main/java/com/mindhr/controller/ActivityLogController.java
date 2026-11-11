package com.mindhr.controller;

import io.swagger.v3.oas.annotations.Operation;
import com.mindhr.dto.ActivityLogRequest;
import com.mindhr.dto.ActivityLogResponse;
import com.mindhr.service.ActivityLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/activity-logs")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @Operation(summary = "Activity log (newest first)",
            description = "Every successful create / update / delete / approve ... is recorded automatically. "
            + "`limit`: default 100, max 500. Platform staff: all organizations, or one with "
            + "`organizationId`. Org users: their own organization, and only with ORGANIZATION data scope "
            + "(owner, HR admin); others get 403.")
    @GetMapping
    public ResponseEntity<List<ActivityLogResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId,
            @RequestParam(name = "limit", required = false) Integer limit) {
        return ResponseEntity.ok(activityLogService.list(organizationId, limit));
    }

    @Operation(summary = "Add a manual activity entry",
            description = "Platform staff only (SUPER_ADMIN, SAAS_STAFF); organization users get 403. Normal activity "
            + "is recorded automatically; use this only for events outside the API.")
    @PostMapping
    public ResponseEntity<ActivityLogResponse> create(@Valid @RequestBody ActivityLogRequest request) {
        return ResponseEntity.status(201).body(activityLogService.create(request));
    }
}

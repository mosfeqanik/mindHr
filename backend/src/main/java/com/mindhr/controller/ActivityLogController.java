package com.mindhr.controller;

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

    @GetMapping
    public ResponseEntity<List<ActivityLogResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId) {
        return ResponseEntity.ok(activityLogService.list(organizationId));
    }

    @PostMapping
    public ResponseEntity<ActivityLogResponse> create(@Valid @RequestBody ActivityLogRequest request) {
        return ResponseEntity.status(201).body(activityLogService.create(request));
    }
}

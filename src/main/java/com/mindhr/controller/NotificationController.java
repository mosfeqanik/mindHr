package com.mindhr.controller;

import com.mindhr.dto.NotificationRequest;
import com.mindhr.dto.NotificationResponse;
import com.mindhr.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(
            @RequestParam(name = "organizationId", required = false) Long organizationId,
            @RequestParam(name = "userId", required = false) Long userId) {
        return ResponseEntity.ok(notificationService.list(organizationId, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.getById(id));
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> create(@Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.status(201).body(notificationService.create(request));
    }

    @PatchMapping("/{id}/sent")
    public ResponseEntity<NotificationResponse> markSent(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markSent(id));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markRead(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        notificationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

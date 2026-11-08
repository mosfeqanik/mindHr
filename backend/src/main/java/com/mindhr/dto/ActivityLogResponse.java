package com.mindhr.dto;

import java.time.LocalDateTime;

public record ActivityLogResponse(
        Long id,
        Long organizationId,
        Long actorUserId,
        String actorName,
        String action,
        String entityType,
        Long entityId,
        String details,
        String ipAddress,
        LocalDateTime createdAt
) {
}

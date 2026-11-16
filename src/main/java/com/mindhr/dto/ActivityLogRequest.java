package com.mindhr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivityLogRequest(

        /** Null for a platform-level action; set to attribute the entry to one organization. */
        Long organizationId,

        Long actorUserId,

        @NotBlank(message = "Action is required")
        @Size(max = 50, message = "Action must be at most 50 characters")
        String action,

        @NotBlank(message = "Entity type is required")
        @Size(max = 50, message = "Entity type must be at most 50 characters")
        String entityType,

        Long entityId,

        String details,

        @Size(max = 45, message = "IP address must be at most 45 characters")
        String ipAddress
) {
}

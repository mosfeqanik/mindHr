package com.mindhr.dto;

import com.mindhr.entity.enums.SalesActivityType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record SalesActivityRequest(

        @NotNull(message = "Lead is required")
        Long leadId,

        Long performedByUserId,

        @NotNull(message = "Type is required")
        SalesActivityType type,

        String note,

        LocalDateTime scheduledAt,

        LocalDateTime completedAt
) {
}

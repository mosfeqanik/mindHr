package com.mindhr.dto;

import com.mindhr.entity.enums.SalesActivityType;

import java.time.LocalDateTime;

public record SalesActivityResponse(
        Long id,
        Long leadId,
        Long performedByUserId,
        SalesActivityType type,
        String note,
        LocalDateTime scheduledAt,
        LocalDateTime completedAt
) {
}

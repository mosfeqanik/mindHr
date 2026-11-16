package com.mindhr.dto;

import com.mindhr.entity.enums.*;

import java.math.BigDecimal;
import java.time.*;

public record WorkRequestResponse(
        Long id,
        Long organizationId,
        Long employeeId,
        RequestType type,
        String title,
        String details,
        BigDecimal amount,
        LocalDate requestDate,
        RequestStatus status,
        Long decidedById,
        LocalDateTime decidedAt,
        String decisionNote
) {
}

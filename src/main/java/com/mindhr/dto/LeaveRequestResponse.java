package com.mindhr.dto;

import com.mindhr.entity.enums.*;

import java.math.BigDecimal;
import java.time.*;

public record LeaveRequestResponse(
        Long id,
        Long organizationId,
        Long employeeId,
        Long leaveTypeId,
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal days,
        String reason,
        LeaveStatus status,
        Long decidedById,
        LocalDateTime decidedAt,
        String decisionNote
) {
}

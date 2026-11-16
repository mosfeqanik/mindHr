package com.mindhr.dto;

import com.mindhr.entity.enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.*;

public record LeaveRequestRequest(

        Long organizationId,

        @NotNull(message = "Employee id is required")
        Long employeeId,

        @NotNull(message = "Leave type id is required")
        Long leaveTypeId,

        @NotNull(message = "From date is required")
        LocalDate fromDate,

        @NotNull(message = "To date is required")
        LocalDate toDate,

        @NotNull(message = "Days is required")
        @DecimalMin(value = "0.0", message = "Days must not be negative")
        BigDecimal days,

        @Size(max = 500, message = "Reason must be at most 500 characters")
        String reason,

        LeaveStatus status,

        Long decidedById,

        LocalDateTime decidedAt,

        @Size(max = 500, message = "Decision note must be at most 500 characters")
        String decisionNote
) {
}

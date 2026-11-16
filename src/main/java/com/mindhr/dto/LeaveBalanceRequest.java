package com.mindhr.dto;

import com.mindhr.entity.enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.*;

public record LeaveBalanceRequest(

        Long organizationId,

        @NotNull(message = "Employee id is required")
        Long employeeId,

        @NotNull(message = "Leave type id is required")
        Long leaveTypeId,

        Integer year,

        @NotNull(message = "Entitled days is required")
        @DecimalMin(value = "0.0", message = "Entitled days must not be negative")
        BigDecimal entitledDays,

        @DecimalMin(value = "0.0", message = "Used days must not be negative")
        BigDecimal usedDays
) {
}

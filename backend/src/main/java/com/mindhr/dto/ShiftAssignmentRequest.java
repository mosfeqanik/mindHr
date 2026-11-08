package com.mindhr.dto;

import com.mindhr.entity.enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.*;

public record ShiftAssignmentRequest(

        Long organizationId,

        @NotNull(message = "Employee id is required")
        Long employeeId,

        Long shiftId,

        @NotNull(message = "Work date is required")
        LocalDate workDate
) {
}

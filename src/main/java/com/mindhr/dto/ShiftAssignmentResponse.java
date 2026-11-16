package com.mindhr.dto;

import com.mindhr.entity.enums.*;

import java.math.BigDecimal;
import java.time.*;

public record ShiftAssignmentResponse(
        Long id,
        Long organizationId,
        Long employeeId,
        Long shiftId,
        LocalDate workDate
) {
}

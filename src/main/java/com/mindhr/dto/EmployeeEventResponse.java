package com.mindhr.dto;

import com.mindhr.entity.enums.*;

import java.math.BigDecimal;
import java.time.*;

public record EmployeeEventResponse(
        Long id,
        Long organizationId,
        Long employeeId,
        EmployeeEventType type,
        LocalDate eventDate,
        String title,
        String details,
        String oldValue,
        String newValue
) {
}

package com.mindhr.dto;

import com.mindhr.entity.enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.*;

public record EmployeeEventRequest(

        Long organizationId,

        @NotNull(message = "Employee id is required")
        Long employeeId,

        @NotNull(message = "Type is required")
        EmployeeEventType type,

        @NotNull(message = "Event date is required")
        LocalDate eventDate,

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        String details,

        @Size(max = 255, message = "Old value must be at most 255 characters")
        String oldValue,

        @Size(max = 255, message = "New value must be at most 255 characters")
        String newValue
) {
}

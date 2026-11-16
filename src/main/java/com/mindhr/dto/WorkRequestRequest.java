package com.mindhr.dto;

import com.mindhr.entity.enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.*;

public record WorkRequestRequest(

        Long organizationId,

        @NotNull(message = "Employee id is required")
        Long employeeId,

        @NotNull(message = "Type is required")
        RequestType type,

        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must be at most 150 characters")
        String title,

        String details,

        @DecimalMin(value = "0.0", message = "Amount must not be negative")
        BigDecimal amount,

        LocalDate requestDate,

        RequestStatus status,

        Long decidedById,

        LocalDateTime decidedAt,

        @Size(max = 500, message = "Decision note must be at most 500 characters")
        String decisionNote
) {
}

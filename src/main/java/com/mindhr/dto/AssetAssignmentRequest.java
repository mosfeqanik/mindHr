package com.mindhr.dto;

import com.mindhr.entity.enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.*;

public record AssetAssignmentRequest(

        Long organizationId,

        @NotNull(message = "Asset id is required")
        Long assetId,

        @NotNull(message = "Employee id is required")
        Long employeeId,

        @NotNull(message = "Assigned at is required")
        LocalDate assignedAt,

        LocalDate returnedAt,

        @Size(max = 500, message = "Condition note must be at most 500 characters")
        String conditionNote
) {
}

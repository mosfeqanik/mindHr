package com.mindhr.dto;

import com.mindhr.entity.enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.*;

public record SalaryStructureRequest(

        Long organizationId,

        @NotNull(message = "Employee id is required")
        Long employeeId,

        @NotNull(message = "Effective from is required")
        LocalDate effectiveFrom,

        @NotNull(message = "Basic salary is required")
        @DecimalMin(value = "0.0", message = "Basic salary must not be negative")
        BigDecimal basicSalary,

        @DecimalMin(value = "0.0", message = "Allowances must not be negative")
        BigDecimal allowances,

        @Size(max = 3, message = "Currency must be at most 3 characters")
        String currency
) {
}

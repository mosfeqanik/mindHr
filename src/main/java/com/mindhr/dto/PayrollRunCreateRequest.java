package com.mindhr.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record PayrollRunCreateRequest(

        Long organizationId,

        @Min(value = 2000, message = "Year is invalid")
        int year,

        @Min(value = 1, message = "Month must be 1-12")
        @Max(value = 12, message = "Month must be 1-12")
        int month,

        /** Null runs payroll for the whole organization. */
        Long branchId,

        @Size(max = 500, message = "Note must be at most 500 characters")
        String note
) {
}

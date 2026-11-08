package com.mindhr.dto;

import com.mindhr.entity.enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.*;

public record PayrollRunRequest(

        Long organizationId,

        Integer year,

        Integer month,

        Long branchId,

        PayrollStatus status,

        @Size(max = 500, message = "Note must be at most 500 characters")
        String note
) {
}

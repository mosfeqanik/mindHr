package com.mindhr.dto;

import com.mindhr.entity.enums.*;

import java.math.BigDecimal;
import java.time.*;

public record PayrollRunResponse(
        Long id,
        Long organizationId,
        int year,
        int month,
        Long branchId,
        PayrollStatus status,
        String note
) {
}

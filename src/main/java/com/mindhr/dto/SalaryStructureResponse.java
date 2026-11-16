package com.mindhr.dto;

import com.mindhr.entity.enums.*;

import java.math.BigDecimal;
import java.time.*;

public record SalaryStructureResponse(
        Long id,
        Long organizationId,
        Long employeeId,
        LocalDate effectiveFrom,
        BigDecimal basicSalary,
        BigDecimal allowances,
        String currency
) {
}

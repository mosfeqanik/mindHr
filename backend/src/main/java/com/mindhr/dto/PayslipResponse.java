package com.mindhr.dto;

import com.mindhr.entity.enums.*;

import java.math.BigDecimal;
import java.time.*;

public record PayslipResponse(
        Long id,
        Long organizationId,
        Long payrollRunId,
        Long employeeId,
        BigDecimal basicSalary,
        BigDecimal allowances,
        BigDecimal overtime,
        BigDecimal bonus,
        BigDecimal commission,
        BigDecimal grossSalary,
        BigDecimal lateDeduction,
        BigDecimal absentDeduction,
        BigDecimal loanDeduction,
        BigDecimal advanceDeduction,
        BigDecimal netSalary,
        int daysPresent,
        int daysAbsent,
        int daysLate,
        PayrollStatus status
) {
}

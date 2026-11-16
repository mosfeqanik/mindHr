package com.mindhr.dto;

import com.mindhr.entity.enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.*;

public record PayslipRequest(

        Long organizationId,

        @NotNull(message = "Payroll run id is required")
        Long payrollRunId,

        @NotNull(message = "Employee id is required")
        Long employeeId,

        @DecimalMin(value = "0.0", message = "Basic salary must not be negative")
        BigDecimal basicSalary,

        @DecimalMin(value = "0.0", message = "Allowances must not be negative")
        BigDecimal allowances,

        @DecimalMin(value = "0.0", message = "Overtime must not be negative")
        BigDecimal overtime,

        @DecimalMin(value = "0.0", message = "Bonus must not be negative")
        BigDecimal bonus,

        @DecimalMin(value = "0.0", message = "Commission must not be negative")
        BigDecimal commission,

        @DecimalMin(value = "0.0", message = "Gross salary must not be negative")
        BigDecimal grossSalary,

        @DecimalMin(value = "0.0", message = "Late deduction must not be negative")
        BigDecimal lateDeduction,

        @DecimalMin(value = "0.0", message = "Absent deduction must not be negative")
        BigDecimal absentDeduction,

        @DecimalMin(value = "0.0", message = "Loan deduction must not be negative")
        BigDecimal loanDeduction,

        @DecimalMin(value = "0.0", message = "Advance deduction must not be negative")
        BigDecimal advanceDeduction,

        @DecimalMin(value = "0.0", message = "Net salary must not be negative")
        BigDecimal netSalary,

        Integer daysPresent,

        Integer daysAbsent,

        Integer daysLate,

        PayrollStatus status
) {
}

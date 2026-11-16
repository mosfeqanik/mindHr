package com.mindhr.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

/** Manual adjustments on a DRAFT payslip. A null field keeps the current value. */
public record PayslipAdjustRequest(
        @DecimalMin(value = "0.0", message = "Overtime must not be negative") BigDecimal overtime,
        @DecimalMin(value = "0.0", message = "Bonus must not be negative") BigDecimal bonus,
        @DecimalMin(value = "0.0", message = "Commission must not be negative") BigDecimal commission,
        @DecimalMin(value = "0.0", message = "Loan deduction must not be negative") BigDecimal loanDeduction,
        @DecimalMin(value = "0.0", message = "Advance deduction must not be negative") BigDecimal advanceDeduction
) {
}

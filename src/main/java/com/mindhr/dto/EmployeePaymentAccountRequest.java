package com.mindhr.dto;

import com.mindhr.entity.enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.*;

public record EmployeePaymentAccountRequest(

        Long organizationId,

        @NotNull(message = "Employee id is required")
        Long employeeId,

        @NotNull(message = "Type is required")
        PaymentAccountType type,

        @Size(max = 100, message = "Provider must be at most 100 characters")
        String provider,

        @Size(max = 150, message = "Account name must be at most 150 characters")
        String accountName,

        @NotBlank(message = "Account number is required")
        @Size(max = 100, message = "Account number must be at most 100 characters")
        String accountNumber,

        Boolean primaryAccount
) {
}

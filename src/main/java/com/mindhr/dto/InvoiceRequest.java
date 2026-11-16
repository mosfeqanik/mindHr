package com.mindhr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceRequest(

        Long organizationId,

        Long subscriptionId,

        @NotBlank(message = "Invoice number is required")
        @Size(max = 30, message = "Invoice number must be at most 30 characters")
        String invoiceNumber,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Amount must not be negative")
        BigDecimal amount,

        @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
        String currency,

        @NotNull(message = "Issue date is required")
        LocalDate issueDate,

        @NotNull(message = "Due date is required")
        LocalDate dueDate,

        String notes
) {
}

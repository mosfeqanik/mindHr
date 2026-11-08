package com.mindhr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RefundRequest(

        @NotNull(message = "Payment is required")
        Long paymentId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be positive")
        BigDecimal amount,

        @Size(max = 500, message = "Reason must be at most 500 characters")
        String reason
) {
}

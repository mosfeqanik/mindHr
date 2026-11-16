package com.mindhr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PaymentRequest(

        Long organizationId,

        @NotNull(message = "Invoice is required")
        Long invoiceId,

        Long paymentMethodId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Amount must not be negative")
        BigDecimal amount,

        @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
        String currency,

        @Size(max = 50, message = "Gateway must be at most 50 characters")
        String gateway,

        @Size(max = 100, message = "Gateway transaction id must be at most 100 characters")
        String gatewayTransactionId
) {
}

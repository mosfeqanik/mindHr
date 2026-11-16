package com.mindhr.dto;

import com.mindhr.entity.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public record PaymentStatusChangeRequest(
        @NotNull(message = "Status is required")
        PaymentStatus status
) {
}

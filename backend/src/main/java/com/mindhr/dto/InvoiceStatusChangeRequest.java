package com.mindhr.dto;

import com.mindhr.entity.enums.InvoiceStatus;
import jakarta.validation.constraints.NotNull;

public record InvoiceStatusChangeRequest(
        @NotNull(message = "Status is required")
        InvoiceStatus status
) {
}

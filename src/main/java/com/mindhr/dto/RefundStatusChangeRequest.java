package com.mindhr.dto;

import com.mindhr.entity.enums.RefundStatus;
import jakarta.validation.constraints.NotNull;

public record RefundStatusChangeRequest(
        @NotNull(message = "Status is required")
        RefundStatus status
) {
}

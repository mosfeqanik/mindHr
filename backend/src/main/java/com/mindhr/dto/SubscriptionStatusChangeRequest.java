package com.mindhr.dto;

import com.mindhr.entity.enums.SubscriptionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubscriptionStatusChangeRequest(
        @NotNull(message = "Status is required")
        SubscriptionStatus status,

        @Size(max = 500, message = "Note must be at most 500 characters")
        String note
) {
}

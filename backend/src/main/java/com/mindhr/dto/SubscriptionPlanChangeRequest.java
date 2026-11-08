package com.mindhr.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubscriptionPlanChangeRequest(
        @NotNull(message = "Plan is required")
        Long planId,

        @Size(max = 500, message = "Note must be at most 500 characters")
        String note
) {
}

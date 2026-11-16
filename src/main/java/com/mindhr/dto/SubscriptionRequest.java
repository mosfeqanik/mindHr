package com.mindhr.dto;

import com.mindhr.entity.enums.SubscriptionStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SubscriptionRequest(

        Long organizationId,

        @NotNull(message = "Plan is required")
        Long planId,

        @NotNull(message = "Status is required")
        SubscriptionStatus status,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        LocalDate endDate,

        LocalDate renewalDate,

        LocalDateTime trialEndsAt,

        Boolean autoRenew
) {
}

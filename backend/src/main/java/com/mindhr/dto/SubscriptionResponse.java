package com.mindhr.dto;

import com.mindhr.entity.enums.SubscriptionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SubscriptionResponse(
        Long id,
        Long organizationId,
        Long planId,
        String planName,
        SubscriptionStatus status,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate renewalDate,
        LocalDateTime trialEndsAt,
        boolean autoRenew,
        LocalDateTime cancelledAt
) {
}

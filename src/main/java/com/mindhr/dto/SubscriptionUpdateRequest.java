package com.mindhr.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Plan and status are changed only via the dedicated /plan and /status actions, so a history row is always recorded. */
public record SubscriptionUpdateRequest(
        LocalDate endDate,
        LocalDate renewalDate,
        LocalDateTime trialEndsAt,
        Boolean autoRenew
) {
}

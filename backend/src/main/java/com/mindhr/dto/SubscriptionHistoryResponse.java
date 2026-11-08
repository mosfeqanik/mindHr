package com.mindhr.dto;

import com.mindhr.entity.enums.SubscriptionStatus;

import java.time.LocalDateTime;

public record SubscriptionHistoryResponse(
        Long id,
        Long subscriptionId,
        Long fromPlanId,
        String fromPlanName,
        Long toPlanId,
        String toPlanName,
        SubscriptionStatus fromStatus,
        SubscriptionStatus toStatus,
        Long changedByUserId,
        String changedByName,
        String note,
        LocalDateTime changedAt
) {
}

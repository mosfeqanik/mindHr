package com.mindhr.dto;

import com.mindhr.entity.enums.PlanFeature;
import com.mindhr.entity.enums.SubscriptionStatus;

import java.util.Set;

/**
 * The plan an organization can use right now. When it has no TRIAL / ACTIVE / GRACE_PERIOD
 * subscription, planId, planName and status are null and features is empty.
 */
public record CurrentPlanResponse(
        Long planId,
        String planName,
        SubscriptionStatus status,
        Set<PlanFeature> features
) {
}

package com.mindhr.dto;

import com.mindhr.entity.enums.BillingCycle;
import com.mindhr.entity.enums.PlanFeature;

import java.math.BigDecimal;
import java.util.Set;

public record SubscriptionPlanResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String currency,
        BillingCycle billingCycle,
        Integer maxEmployees,
        Integer maxBranches,
        Set<PlanFeature> features,
        int trialDays,
        boolean active
) {
}

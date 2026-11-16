package com.mindhr.mapper;

import com.mindhr.dto.SubscriptionPlanResponse;
import com.mindhr.entity.SubscriptionPlan;

public final class SubscriptionPlanMapper {

    private SubscriptionPlanMapper() {
    }

    public static SubscriptionPlanResponse toResponse(SubscriptionPlan plan) {
        return new SubscriptionPlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getDescription(),
                plan.getPrice(),
                plan.getCurrency(),
                plan.getBillingCycle(),
                plan.getMaxEmployees(),
                plan.getMaxBranches(),
                plan.getFeatures(),
                plan.getTrialDays(),
                plan.isActive()
        );
    }
}

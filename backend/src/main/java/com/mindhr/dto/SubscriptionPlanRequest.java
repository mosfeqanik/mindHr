package com.mindhr.dto;

import com.mindhr.entity.enums.BillingCycle;
import com.mindhr.entity.enums.PlanFeature;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;

public record SubscriptionPlanRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Price must not be negative")
        BigDecimal price,

        @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
        String currency,

        @NotNull(message = "Billing cycle is required")
        BillingCycle billingCycle,

        @Positive(message = "Max employees must be positive")
        Integer maxEmployees,

        @Positive(message = "Max branches must be positive")
        Integer maxBranches,

        Set<PlanFeature> features,

        @PositiveOrZero(message = "Trial days must not be negative")
        Integer trialDays,

        Boolean active
) {
}

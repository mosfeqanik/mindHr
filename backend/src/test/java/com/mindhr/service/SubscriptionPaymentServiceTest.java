package com.mindhr.service;

import com.mindhr.entity.SubscriptionPlan;
import com.mindhr.entity.enums.BillingCycle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubscriptionPaymentServiceTest {

    private static SubscriptionPlan plan(String price, BillingCycle cycle) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setPrice(new BigDecimal(price));
        plan.setBillingCycle(cycle);
        return plan;
    }

    @Test
    void monthlyPlanChargesFullPrice() {
        assertEquals(new BigDecimal("1500.00"), SubscriptionPaymentService.monthlyAmount(plan("1500", BillingCycle.MONTHLY)));
    }

    @Test
    void yearlyPlanChargesOneTwelfthRounded() {
        assertEquals(new BigDecimal("833.33"), SubscriptionPaymentService.monthlyAmount(plan("10000", BillingCycle.YEARLY)));
    }
}

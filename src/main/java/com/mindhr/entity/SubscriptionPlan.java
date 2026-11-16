package com.mindhr.entity;

import com.mindhr.entity.base.BaseEntity;
import com.mindhr.entity.enums.BillingCycle;
import com.mindhr.entity.enums.PlanFeature;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency = "BDT";

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 30)
    private BillingCycle billingCycle;

    /** Null means unlimited employees. */
    @Column(name = "max_employees")
    private Integer maxEmployees;

    /** Null means unlimited branches. */
    @Column(name = "max_branches")
    private Integer maxBranches;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "subscription_plan_features", joinColumns = @JoinColumn(name = "plan_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "feature", nullable = false, length = 30)
    private Set<PlanFeature> features = new HashSet<>();

    @Column(name = "trial_days", nullable = false)
    private int trialDays = 0;

    @Column(nullable = false)
    private boolean active = true;
}

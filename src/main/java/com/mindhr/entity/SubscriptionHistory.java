package com.mindhr.entity;

import com.mindhr.entity.base.BaseEntity;
import com.mindhr.entity.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Tracks upgrades, downgrades, renewals and status changes of a subscription. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "subscription_history")
public class SubscriptionHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_plan_id")
    private SubscriptionPlan fromPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_plan_id")
    private SubscriptionPlan toPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private SubscriptionStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private SubscriptionStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id")
    private User changedBy;

    @Column(length = 500)
    private String note;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
}

package com.mindhr.service;

import com.mindhr.dto.CurrentPlanResponse;
import com.mindhr.entity.Subscription;
import com.mindhr.entity.SubscriptionPlan;
import com.mindhr.entity.enums.PlanFeature;
import com.mindhr.entity.enums.SubscriptionStatus;
import com.mindhr.exception.PlanLimitExceededException;
import com.mindhr.repository.EmployeeRepository;
import com.mindhr.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Enforces what an organization's subscription plan allows: employee and branch limits and
 * feature switches. A plan limit of {@code null} means unlimited.
 */
@Service
@RequiredArgsConstructor
public class PlanLimitService {

    /** Statuses in which an organization may still use the product (grace period keeps access). */
    private static final List<SubscriptionStatus> USABLE_STATUSES =
            List.of(SubscriptionStatus.TRIAL, SubscriptionStatus.ACTIVE, SubscriptionStatus.GRACE_PERIOD);

    private final SubscriptionRepository subscriptionRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public void assertCanAddEmployee(Long organizationId) {
        SubscriptionPlan plan = currentPlan(organizationId);
        Integer max = plan.getMaxEmployees();
        if (max != null && employeeRepository.countByOrganizationId(organizationId) >= max) {
            throw new PlanLimitExceededException(
                    "Employee limit of " + max + " reached for plan " + plan.getName());
        }
    }

    /** {@code currentBranchCount} is passed in so this service does not depend on the branch module. */
    @Transactional(readOnly = true)
    public void assertCanAddBranch(Long organizationId, long currentBranchCount) {
        SubscriptionPlan plan = currentPlan(organizationId);
        Integer max = plan.getMaxBranches();
        if (max != null && currentBranchCount >= max) {
            throw new PlanLimitExceededException(
                    "Branch limit of " + max + " reached for plan " + plan.getName());
        }
    }

    @Transactional(readOnly = true)
    public void assertFeature(Long organizationId, PlanFeature feature) {
        SubscriptionPlan plan = currentPlan(organizationId);
        if (!plan.getFeatures().contains(feature)) {
            throw new PlanLimitExceededException(
                    "Plan " + plan.getName() + " does not include " + feature);
        }
    }

    /** What the organization's current plan includes; the web app shows only those modules. */
    @Transactional(readOnly = true)
    public CurrentPlanResponse currentPlanInfo(Long organizationId) {
        return currentSubscription(organizationId)
                .map(s -> new CurrentPlanResponse(s.getPlan().getId(), s.getPlan().getName(), s.getStatus(),
                        Set.copyOf(s.getPlan().getFeatures())))
                .orElseGet(() -> new CurrentPlanResponse(null, null, null, Set.of()));
    }

    private SubscriptionPlan currentPlan(Long organizationId) {
        return currentSubscription(organizationId)
                .map(Subscription::getPlan)
                .orElseThrow(() -> new PlanLimitExceededException(
                        "Organization has no active subscription"));
    }

    private Optional<Subscription> currentSubscription(Long organizationId) {
        return subscriptionRepository.findByOrganizationIdAndStatusIn(organizationId, USABLE_STATUSES).stream()
                .max(Comparator.comparing(Subscription::getCreatedAt));
    }
}

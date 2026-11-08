package com.mindhr.service;

import com.mindhr.dto.SubscriptionHistoryResponse;
import com.mindhr.dto.SubscriptionPlanChangeRequest;
import com.mindhr.dto.SubscriptionRequest;
import com.mindhr.dto.SubscriptionResponse;
import com.mindhr.dto.SubscriptionStatusChangeRequest;
import com.mindhr.dto.SubscriptionUpdateRequest;
import com.mindhr.entity.Organization;
import com.mindhr.entity.Subscription;
import com.mindhr.entity.SubscriptionHistory;
import com.mindhr.entity.SubscriptionPlan;
import com.mindhr.entity.User;
import com.mindhr.entity.enums.OrganizationStatus;
import com.mindhr.entity.enums.SubscriptionStatus;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.SubscriptionHistoryMapper;
import com.mindhr.mapper.SubscriptionMapper;
import com.mindhr.repository.OrganizationRepository;
import com.mindhr.repository.SubscriptionHistoryRepository;
import com.mindhr.repository.SubscriptionPlanRepository;
import com.mindhr.repository.SubscriptionRepository;
import com.mindhr.repository.UserRepository;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Plan and status changes always go through {@link #changeStatus} / {@link #changePlan} rather
 * than a generic update, so every transition is recorded as a {@link SubscriptionHistory} row.
 * There is no delete: a subscription is ended by moving its status to {@code CANCELLED}, not by
 * removing the billing record.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public List<SubscriptionResponse> list(Long requestedOrganizationId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        return subscriptionRepository.findByOrganizationId(organizationId).stream()
                .map(SubscriptionMapper::toResponse)
                .toList();
    }

    public SubscriptionResponse getById(Long id) {
        return SubscriptionMapper.toResponse(findAccessibleEntity(id));
    }

    public List<SubscriptionHistoryResponse> history(Long id) {
        findAccessibleEntity(id);
        return subscriptionHistoryRepository.findBySubscriptionIdOrderByChangedAtDesc(id).stream()
                .map(SubscriptionHistoryMapper::toResponse)
                .toList();
    }

    @Transactional
    public SubscriptionResponse create(SubscriptionRequest request) {
        Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        SubscriptionPlan plan = findPlan(request.planId());

        Subscription subscription = new Subscription();
        subscription.setOrganization(organization);
        subscription.setPlan(plan);
        subscription.setStatus(request.status());
        subscription.setStartDate(request.startDate());
        subscription.setEndDate(request.endDate());
        subscription.setRenewalDate(request.renewalDate());
        subscription.setTrialEndsAt(request.trialEndsAt());
        subscription.setAutoRenew(request.autoRenew() == null || request.autoRenew());

        subscription = subscriptionRepository.save(subscription);
        recordHistory(subscription, null, subscription.getStatus(), plan, plan, "Subscription created");
        return SubscriptionMapper.toResponse(subscription);
    }

    /**
     * The first subscription, created together with the organization. TRIAL when the organization
     * is on trial, otherwise ACTIVE. Without an explicit trial end, the plan's trial days are used.
     */
    @Transactional
    public Subscription createInitial(Organization organization, Long planId, LocalDateTime trialEndsAt) {
        SubscriptionPlan plan = findPlan(planId);
        if (!plan.isActive()) {
            throw new IllegalArgumentException("Subscription plan is not active: " + plan.getName());
        }
        if (trialEndsAt == null && plan.getTrialDays() > 0) {
            trialEndsAt = LocalDateTime.now().plusDays(plan.getTrialDays());
        }

        Subscription subscription = new Subscription();
        subscription.setOrganization(organization);
        subscription.setPlan(plan);
        subscription.setStatus(organization.getStatus() == OrganizationStatus.TRIAL
                ? SubscriptionStatus.TRIAL : SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDate.now());
        subscription.setTrialEndsAt(trialEndsAt);
        subscription.setAutoRenew(true);

        subscription = subscriptionRepository.save(subscription);
        recordHistory(subscription, null, subscription.getStatus(), plan, plan, "Subscription created with organization");
        return subscription;
    }

    @Transactional
    public SubscriptionResponse update(Long id, SubscriptionUpdateRequest request) {
        Subscription subscription = findAccessibleEntity(id);
        subscription.setEndDate(request.endDate());
        subscription.setRenewalDate(request.renewalDate());
        subscription.setTrialEndsAt(request.trialEndsAt());
        if (request.autoRenew() != null) {
            subscription.setAutoRenew(request.autoRenew());
        }
        return SubscriptionMapper.toResponse(subscription);
    }

    @Transactional
    public SubscriptionResponse changeStatus(Long id, SubscriptionStatusChangeRequest request) {
        Subscription subscription = findAccessibleEntity(id);
        SubscriptionStatus fromStatus = subscription.getStatus();
        subscription.setStatus(request.status());
        if (request.status() == SubscriptionStatus.CANCELLED) {
            subscription.setCancelledAt(LocalDateTime.now());
        }
        recordHistory(subscription, fromStatus, request.status(), subscription.getPlan(), subscription.getPlan(), request.note());
        return SubscriptionMapper.toResponse(subscription);
    }

    @Transactional
    public SubscriptionResponse changePlan(Long id, SubscriptionPlanChangeRequest request) {
        Subscription subscription = findAccessibleEntity(id);
        SubscriptionPlan fromPlan = subscription.getPlan();
        SubscriptionPlan toPlan = findPlan(request.planId());
        subscription.setPlan(toPlan);
        recordHistory(subscription, subscription.getStatus(), subscription.getStatus(), fromPlan, toPlan, request.note());
        return SubscriptionMapper.toResponse(subscription);
    }

    private void recordHistory(Subscription subscription, SubscriptionStatus fromStatus, SubscriptionStatus toStatus,
                                SubscriptionPlan fromPlan, SubscriptionPlan toPlan, String note) {
        SubscriptionHistory history = new SubscriptionHistory();
        history.setSubscription(subscription);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setFromPlan(fromPlan);
        history.setToPlan(toPlan);
        history.setNote(note);
        history.setChangedAt(LocalDateTime.now());
        userRepository.findById(TenantContext.currentPrincipal().userId()).ifPresent(history::setChangedBy);
        subscriptionHistoryRepository.save(history);
    }

    private SubscriptionPlan findPlan(Long planId) {
        return subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found: " + planId));
    }

    private Subscription findAccessibleEntity(Long id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + id));
        if (!TenantContext.canAccess(subscription.getOrganization().getId())) {
            throw new ResourceNotFoundException("Subscription not found: " + id);
        }
        return subscription;
    }
}

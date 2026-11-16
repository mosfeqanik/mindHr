package com.mindhr.service;

import com.mindhr.dto.SubscriptionPlanRequest;
import com.mindhr.dto.SubscriptionPlanResponse;
import com.mindhr.entity.SubscriptionPlan;
import com.mindhr.exception.DuplicateResourceException;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.SubscriptionPlanMapper;
import com.mindhr.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public List<SubscriptionPlanResponse> listAll(boolean activeOnly) {
        List<SubscriptionPlan> plans = activeOnly
                ? subscriptionPlanRepository.findByActiveTrue()
                : subscriptionPlanRepository.findAll();
        return plans.stream().map(SubscriptionPlanMapper::toResponse).toList();
    }

    public SubscriptionPlanResponse getById(Long id) {
        return SubscriptionPlanMapper.toResponse(findEntity(id));
    }

    @Transactional
    public SubscriptionPlanResponse create(SubscriptionPlanRequest request) {
        if (subscriptionPlanRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("A subscription plan named '" + request.name() + "' already exists");
        }

        SubscriptionPlan plan = new SubscriptionPlan();
        applyRequest(plan, request);
        return SubscriptionPlanMapper.toResponse(subscriptionPlanRepository.save(plan));
    }

    @Transactional
    public SubscriptionPlanResponse update(Long id, SubscriptionPlanRequest request) {
        SubscriptionPlan plan = findEntity(id);

        if (subscriptionPlanRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new DuplicateResourceException("A subscription plan named '" + request.name() + "' already exists");
        }

        applyRequest(plan, request);
        return SubscriptionPlanMapper.toResponse(plan);
    }

    @Transactional
    public void delete(Long id) {
        if (!subscriptionPlanRepository.existsById(id)) {
            throw new ResourceNotFoundException("Subscription plan not found: " + id);
        }
        subscriptionPlanRepository.deleteById(id);
    }

    private void applyRequest(SubscriptionPlan plan, SubscriptionPlanRequest request) {
        plan.setName(request.name());
        plan.setDescription(request.description());
        plan.setPrice(request.price());
        plan.setCurrency(request.currency() != null ? request.currency().toUpperCase() : "BDT");
        plan.setBillingCycle(request.billingCycle());
        plan.setMaxEmployees(request.maxEmployees());
        plan.setMaxBranches(request.maxBranches());
        plan.setFeatures(request.features() != null ? new HashSet<>(request.features()) : new HashSet<>());
        plan.setTrialDays(request.trialDays() != null ? request.trialDays() : 0);
        plan.setActive(request.active() == null || request.active());
    }

    private SubscriptionPlan findEntity(Long id) {
        return subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found: " + id));
    }
}

package com.mindhr.service;

import com.mindhr.dto.OrganizationRequest;
import com.mindhr.dto.OrganizationResponse;
import com.mindhr.entity.Organization;
import com.mindhr.entity.Subscription;
import com.mindhr.entity.User;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.OrganizationMapper;
import com.mindhr.repository.OrganizationRepository;
import com.mindhr.repository.UserRepository;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Platform-managed: {@code Organization} rows are created/edited by SaaS staff during onboarding
 * (Lead -> Demo -> Organization -> Owner User -> Subscription), never by org users themselves.
 * An org user may only read their own organization.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationService {

    /** Column is 100 chars; leaves room for a "-NNN" suffix. */
    private static final int MAX_SLUG_BASE = 90;

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrgRoleProvisioner roleProvisioner;
    private final SubscriptionService subscriptionService;

    public List<OrganizationResponse> listAll() {
        TenantContext.requirePlatformStaff();
        return organizationRepository.findAll().stream().map(OrganizationMapper::toResponse).toList();
    }

    public OrganizationResponse getById(Long id) {
        Organization organization = findEntity(id);
        if (!TenantContext.canAccess(organization.getId())) {
            throw new ResourceNotFoundException("Organization not found: " + id);
        }
        return OrganizationMapper.toResponse(organization);
    }

    @Transactional
    public OrganizationResponse create(OrganizationRequest request) {
        TenantContext.requirePlatformStaff();
        if (request.planId() == null) {
            throw new IllegalArgumentException("Subscription plan is required");
        }

        Organization organization = new Organization();
        organization.setSlug(uniqueSlug(request.name()));
        applyRequest(organization, request);
        Organization saved = organizationRepository.save(organization);
        roleProvisioner.provision(saved);
        // Every organization starts with a subscription; keep the trial end in step with it.
        Subscription subscription = subscriptionService.createInitial(saved, request.planId(), request.trialEndsAt());
        saved.setTrialEndsAt(subscription.getTrialEndsAt());
        return OrganizationMapper.toResponse(saved);
    }

    @Transactional
    public OrganizationResponse update(Long id, OrganizationRequest request) {
        TenantContext.requirePlatformStaff();
        Organization organization = findEntity(id);
        // The slug is set once at creation and kept on rename, so it stays a stable identifier.
        applyRequest(organization, request);
        return OrganizationMapper.toResponse(organization);
    }

    /** Links the owner user once onboarding creates it; matches the deferred owner_user_id FK. */
    @Transactional
    public OrganizationResponse setOwner(Long id, Long ownerUserId) {
        TenantContext.requirePlatformStaff();
        Organization organization = findEntity(id);
        User owner = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + ownerUserId));
        organization.setOwner(owner);
        roleProvisioner.grantOwnerRole(organization, owner);
        return OrganizationMapper.toResponse(organization);
    }

    @Transactional
    public void delete(Long id) {
        TenantContext.requirePlatformStaff();
        if (!organizationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Organization not found: " + id);
        }
        organizationRepository.deleteById(id);
    }

    private void applyRequest(Organization organization, OrganizationRequest request) {
        organization.setName(request.name());
        organization.setStatus(request.status());
        organization.setEmail(request.email());
        organization.setPhone(request.phone());
        organization.setAddress(request.address());
        organization.setTrialEndsAt(request.trialEndsAt());
        if (request.status().name().equals("ACTIVE") && organization.getActivatedAt() == null) {
            organization.setActivatedAt(java.time.LocalDateTime.now());
        }
    }

    /** "Acme Ltd." -> "acme-ltd"; adds "-2", "-3", ... when the slug is already taken. */
    private String uniqueSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (base.isEmpty()) {
            base = "org";
        }
        base = base.substring(0, Math.min(base.length(), MAX_SLUG_BASE)).replaceAll("-+$", "");

        String slug = base;
        for (int n = 2; organizationRepository.existsBySlugIgnoreCase(slug); n++) {
            slug = base + "-" + n;
        }
        return slug;
    }

    private Organization findEntity(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
    }
}

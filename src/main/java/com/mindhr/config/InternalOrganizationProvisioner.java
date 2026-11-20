package com.mindhr.config;

import com.mindhr.entity.Organization;
import com.mindhr.entity.enums.OrganizationStatus;
import com.mindhr.repository.OrganizationRepository;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Creates mindHR's own organization once: the home of mindHR's staff HR data (employees, attendance, leave,
 * shifts, payroll, assets). Platform users' HR modules always work on it ({@link TenantContext#resolveHrOrganizationId}).
 * It is hidden from the client organization list and has no subscription or plan limits. Runs before
 * {@link OrgRoleBackfillRunner}, which gives it the built-in organization roles.
 */
@Slf4j
@Component
@Order(60)
@RequiredArgsConstructor
public class InternalOrganizationProvisioner implements ApplicationRunner {

    public static final String SLUG = "mindhr-internal";

    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Organization internal = organizationRepository.findFirstByInternalTrue().orElseGet(() -> {
            Organization o = new Organization();
            o.setName("mindHR");
            o.setSlug(SLUG);
            o.setStatus(OrganizationStatus.ACTIVE);
            o.setActivatedAt(LocalDateTime.now());
            o.setInternal(true);
            log.info("Creating mindHR's internal organization for staff HR");
            return organizationRepository.save(o);
        });
        TenantContext.setInternalOrganizationId(internal.getId());
        log.info("mindHR internal organization: id {}", internal.getId());
    }
}

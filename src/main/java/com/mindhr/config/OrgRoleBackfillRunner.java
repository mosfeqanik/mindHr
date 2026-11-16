package com.mindhr.config;

import com.mindhr.entity.Organization;
import com.mindhr.repository.OrganizationRepository;
import com.mindhr.service.OrgRoleProvisioner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gives organizations that existed before the HR roles were introduced their built-in roles,
 * and the owner the ORG_OWNER role. Idempotent; runs after {@link DataSeeder}.
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class OrgRoleBackfillRunner implements ApplicationRunner {

    private final OrganizationRepository organizationRepository;
    private final OrgRoleProvisioner provisioner;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Organization organization : organizationRepository.findAll()) {
            provisioner.provision(organization);
            if (organization.getOwner() != null) {
                provisioner.grantOwnerRole(organization, organization.getOwner());
            }
        }
        log.info("Organization roles are up to date");
    }
}

package com.mindhr.config;

import com.mindhr.entity.Role;
import com.mindhr.entity.enums.DataScope;
import com.mindhr.entity.enums.RoleScope;
import com.mindhr.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

/**
 * Built-in platform roles for mindHR's own staff (user type SAAS_STAFF, shown as "mindHR Staff"). They have no
 * organization, so they never mix with a client's roles of the same name. Platform pages are guarded by user type,
 * not by these roles' permissions, so the roles carry none yet: they record what each staff member does.
 * Idempotent: an existing role (matched by name among platform roles) is left as it is.
 */
@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class PlatformRoleProvisioner implements ApplicationRunner {

    private record RoleDef(String name, String description) {
    }

    public static final List<String> STAFF_ROLE_NAMES = List.of("HR_ADMIN", "ACCOUNTS", "TEAM_LEAD", "EMPLOYEE");

    private static final List<RoleDef> DEFINITIONS = List.of(
            new RoleDef("HR_ADMIN", "mindHR staff: manages mindHR's own people and onboarding of client organizations"),
            new RoleDef("ACCOUNTS", "mindHR staff: subscriptions, invoices, payments, receipts and refunds"),
            new RoleDef("TEAM_LEAD", "mindHR staff: leads a support or sales team and assigns tickets"),
            new RoleDef("EMPLOYEE", "mindHR staff: everyday work such as support tickets and contracts"));

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Role> platformRoles = roleRepository.findByOrganizationIdIsNull();
        int created = 0;
        for (RoleDef def : DEFINITIONS) {
            boolean exists = platformRoles.stream().anyMatch(r -> r.getName().equalsIgnoreCase(def.name()));
            if (exists) {
                continue;
            }
            Role role = new Role();
            role.setOrganization(null);
            role.setName(def.name());
            role.setDescription(def.description());
            role.setScope(RoleScope.PLATFORM);
            role.setDataScope(DataScope.PLATFORM);
            role.setSystem(true);
            role.setPermissions(new HashSet<>());
            roleRepository.save(role);
            created++;
        }
        log.info("mindHR staff roles are up to date ({} created)", created);
    }
}

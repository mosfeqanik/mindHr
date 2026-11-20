package com.mindhr.config;

import com.mindhr.entity.Role;
import com.mindhr.entity.enums.DataScope;
import com.mindhr.entity.enums.PermissionAction;
import com.mindhr.entity.enums.RoleScope;
import com.mindhr.repository.RoleRepository;
import com.mindhr.service.OrgRoleProvisioner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Built-in platform roles for mindHR's own users (no organization, so they never mix with a client's roles of the
 * same name). Platform pages are guarded by user type; these roles decide what a user may do in the HR modules,
 * which for platform users always work on mindHR's own staff (the internal organization). Each role reuses the
 * matching organization role's HR permissions and data scope.
 *
 * <p>Idempotent: missing roles are created, existing ones only gain missing permissions (nothing is removed), and a
 * role still on the old PLATFORM data scope gets its HR data scope.
 */
@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class PlatformRoleProvisioner implements ApplicationRunner {

    private record RoleDef(String name, String description, DataScope dataScope,
                           Map<String, Set<PermissionAction>> grants) {
    }

    public static final List<String> STAFF_ROLE_NAMES = List.of("ADMIN", "HR_ADMIN", "ACCOUNTS", "TEAM_LEAD", "EMPLOYEE");

    private static Map<String, Set<PermissionAction>> accountsGrants() {
        // Own self-service like any employee, plus running payroll for all of mindHR's staff.
        Map<String, Set<PermissionAction>> grants = OrgRoleProvisioner.grantsOf(OrgRoleProvisioner.EMPLOYEE);
        grants.put("PAYROLL", EnumSet.allOf(PermissionAction.class));
        grants.put("PAYSLIP", EnumSet.allOf(PermissionAction.class));
        return grants;
    }

    private static final List<RoleDef> DEFINITIONS = List.of(
            new RoleDef("ADMIN", "mindHR admin (user type Admin): manages users, the platform and mindHR's staff HR",
                    DataScope.ORGANIZATION, OrgRoleProvisioner.allGrants()),
            new RoleDef("HR_ADMIN", "mindHR staff: manages mindHR's own people (employees, leave, attendance, shifts)",
                    DataScope.ORGANIZATION, OrgRoleProvisioner.grantsOf(OrgRoleProvisioner.HR_ADMIN)),
            new RoleDef("ACCOUNTS", "mindHR staff: subscriptions, invoices, payments, refunds and mindHR's payroll",
                    DataScope.ORGANIZATION, accountsGrants()),
            new RoleDef("TEAM_LEAD", "mindHR staff: leads a team, assigns tickets and approves the team's leave",
                    DataScope.TEAM, OrgRoleProvisioner.grantsOf(OrgRoleProvisioner.TEAM_LEAD)),
            new RoleDef("EMPLOYEE", "mindHR staff: everyday work; own attendance, leave, payslips and requests",
                    DataScope.SELF, OrgRoleProvisioner.grantsOf(OrgRoleProvisioner.EMPLOYEE)));

    private final RoleRepository roleRepository;
    private final OrgRoleProvisioner orgRoleProvisioner;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Role> platformRoles = roleRepository.findByOrganizationIdIsNull();
        int created = 0;
        for (RoleDef def : DEFINITIONS) {
            Role role = platformRoles.stream().filter(r -> r.getName().equalsIgnoreCase(def.name())).findFirst()
                    .orElse(null);
            if (role == null) {
                role = new Role();
                role.setOrganization(null);
                role.setName(def.name());
                role.setDescription(def.description());
                role.setScope(RoleScope.PLATFORM);
                role.setSystem(true);
                role.setPermissions(new HashSet<>());
                created++;
            }
            if (role.getDataScope() == null || role.getDataScope() == DataScope.PLATFORM) {
                role.setDataScope(def.dataScope());
            }
            orgRoleProvisioner.addGrants(role, def.grants());
            roleRepository.save(role);
        }
        log.info("mindHR staff roles are up to date ({} created)", created);
    }
}

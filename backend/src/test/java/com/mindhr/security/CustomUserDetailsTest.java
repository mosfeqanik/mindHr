package com.mindhr.security;

import com.mindhr.entity.Organization;
import com.mindhr.entity.Permission;
import com.mindhr.entity.Role;
import com.mindhr.entity.User;
import com.mindhr.entity.enums.DataScope;
import com.mindhr.entity.enums.PermissionAction;
import com.mindhr.entity.enums.UserType;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** The JWT is built from these authorities, so they decide what a user can do after login. */
class CustomUserDetailsTest {

    private static final Organization ORG = org(1L);
    private static final Organization OTHER_ORG = org(2L);

    @Test
    void customOrgRoleNamedSuperAdminGivesNoSuperAdminRole() {
        Role lookalike = role(10L, ORG, "SUPER_ADMIN", DataScope.SELF, permission("LEAVE", PermissionAction.VIEW));
        CustomUserDetails details = CustomUserDetails.fromUser(user(UserType.ORG_USER, ORG, lookalike), null);

        assertThat(authorities(details)).containsExactly("LEAVE_VIEW");
    }

    @Test
    void platformRoleOnAnOrgUserGrantsNothing() {
        Role platformSuperAdmin = role(1L, null, "SUPER_ADMIN", null, permission("ORGANIZATION", PermissionAction.MANAGE));
        CustomUserDetails details = CustomUserDetails.fromUser(user(UserType.ORG_USER, ORG, platformSuperAdmin), null);

        assertThat(authorities(details)).isEmpty();
        assertThat(details.getDataScope()).isEqualTo(DataScope.SELF);
    }

    @Test
    void anotherOrganizationsRoleGrantsNothing() {
        Role foreignOwner = role(20L, OTHER_ORG, "ORG_OWNER", DataScope.ORGANIZATION, permission("PAYROLL", PermissionAction.APPROVE));
        Role ownEmployee = role(21L, ORG, "EMPLOYEE", DataScope.SELF, permission("LEAVE", PermissionAction.CREATE));
        CustomUserDetails details = CustomUserDetails.fromUser(user(UserType.ORG_USER, ORG, foreignOwner, ownEmployee), null);

        assertThat(authorities(details)).containsExactly("LEAVE_CREATE");
        assertThat(details.getDataScope()).isEqualTo(DataScope.SELF);
    }

    @Test
    void orgUserNeverGetsPlatformDataScope() {
        Role wide = role(30L, ORG, "Wide", DataScope.PLATFORM);
        CustomUserDetails details = CustomUserDetails.fromUser(user(UserType.ORG_USER, ORG, wide), null);

        assertThat(details.getDataScope()).isEqualTo(DataScope.ORGANIZATION);
    }

    @Test
    void superAdminRoleComesFromUserType() {
        // No roles at all: a Super Admin created without the seeded role still passes hasRole('SUPER_ADMIN').
        CustomUserDetails details = CustomUserDetails.fromUser(user(UserType.SUPER_ADMIN, null), null);

        assertThat(authorities(details)).contains("ROLE_SUPER_ADMIN").doesNotContain("ROLE_SAAS_STAFF");
        assertThat(details.getDataScope()).isEqualTo(DataScope.PLATFORM);
    }

    @Test
    void saasStaffGetsOnlyItsTypeRoleAndPlatformRolePermissions() {
        Role staff = role(40L, null, "Finance", null, permission("INVOICE", PermissionAction.VIEW));
        Role orgRole = role(41L, ORG, "SUPER_ADMIN", DataScope.ORGANIZATION, permission("PAYROLL", PermissionAction.VIEW));
        CustomUserDetails details = CustomUserDetails.fromUser(user(UserType.SAAS_STAFF, null, staff, orgRole), null);

        assertThat(authorities(details)).containsExactlyInAnyOrder("ROLE_SAAS_STAFF", "INVOICE_VIEW");
    }

    private static Set<String> authorities(CustomUserDetails details) {
        return details.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    private static Organization org(Long id) {
        Organization organization = new Organization();
        organization.setId(id);
        return organization;
    }

    private static Permission permission(String module, PermissionAction action) {
        Permission permission = new Permission();
        permission.setModule(module);
        permission.setAction(action);
        return permission;
    }

    private static Role role(Long id, Organization organization, String name, DataScope dataScope, Permission... permissions) {
        Role role = new Role();
        role.setId(id);
        role.setOrganization(organization);
        role.setName(name);
        role.setDataScope(dataScope);
        role.setPermissions(Set.of(permissions));
        return role;
    }

    private static User user(UserType type, Organization organization, Role... roles) {
        User user = new User();
        user.setId(99L);
        user.setEmail("u@test");
        user.setPasswordHash("x");
        user.setUserType(type);
        user.setOrganization(organization);
        user.setActive(true);
        user.setRoles(Set.of(roles));
        return user;
    }
}

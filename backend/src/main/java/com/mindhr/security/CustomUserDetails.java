package com.mindhr.security;

import com.mindhr.entity.Permission;
import com.mindhr.entity.Role;
import com.mindhr.entity.Employee;
import com.mindhr.entity.enums.DataScope;
import com.mindhr.entity.User;
import com.mindhr.entity.enums.PermissionAction;
import com.mindhr.entity.enums.UserType;
import com.mindhr.service.OrgRoleProvisioner;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Spring Security principal wrapping a {@link User}. Authorities are derived from the
 * user's {@link Role}s ({@code ROLE_<name>}) and each role's {@link Permission}s
 * ({@code <MODULE>_<ACTION>}), matching the CLAUDE.md permission model
 * ({@code Role -> Permission(module, action)}).
 *
 * <p>Must be built from a {@link User} whose {@code roles} and {@code roles.permissions}
 * collections are already initialized (e.g. via
 * {@code UserRepository#findByEmailFetchingRolesAndPermissions}) — both are lazy
 * {@code @ManyToMany} collections and this class does not trigger their loading.
 */
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final String fullName;
    private final UserType userType;
    private final Long organizationId;
    private final boolean active;
    private final Set<GrantedAuthority> authorities;
    private final Long employeeId;
    private final Long branchId;
    private final DataScope dataScope;

    private CustomUserDetails(User user, Employee employee) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.fullName = user.getFullName();
        this.userType = user.getUserType();
        // getId() on a lazy @ManyToOne proxy never hits the database, so this is safe
        // even once the loading transaction has closed.
        this.organizationId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        this.active = user.isActive();
        this.authorities = buildAuthorities(user.getRoles(), user.getUserType());
        this.employeeId = employee != null ? employee.getId() : null;
        this.branchId = employee != null && employee.getBranch() != null ? employee.getBranch().getId() : null;
        this.dataScope = widestScope(user);
    }

    /** {@code employee} is the user's employee record, or null for users without one (e.g. SaaS staff). */
    public static CustomUserDetails fromUser(User user, Employee employee) {
        return new CustomUserDetails(user, employee);
    }

    /** Platform users are always PLATFORM; otherwise the widest scope among the user's roles. */
    private static DataScope widestScope(User user) {
        if (user.getUserType() != UserType.ORG_USER) {
            return DataScope.PLATFORM;
        }
        return user.getRoles().stream()
                .map(Role::effectiveDataScope)
                .reduce(DataScope::widest)
                .orElse(DataScope.SELF);
    }

    private static Set<GrantedAuthority> buildAuthorities(Set<Role> roles, UserType userType) {
        Stream<String> roleAuthorities = roles.stream()
                .map(role -> "ROLE_" + role.getName().toUpperCase().replace(' ', '_'));

        Stream<String> permissionAuthorities = roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getModule().toUpperCase() + "_" + permission.getAction().name());

        return Stream.of(roleAuthorities, permissionAuthorities, superAdminHrAuthorities(userType))
                .flatMap(s -> s)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * The Super Admin may work inside any organization's HR modules, so it holds every HR
     * permission. Organization isolation still applies: platform staff must name the
     * organization (see {@link TenantContext#resolveOrganizationId}).
     */
    private static Stream<String> superAdminHrAuthorities(UserType userType) {
        if (userType != UserType.SUPER_ADMIN) {
            return Stream.empty();
        }
        return OrgRoleProvisioner.MODULES.stream()
                .flatMap(module -> Arrays.stream(PermissionAction.values()).map(action -> module + "_" + action.name()));
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public UserType getUserType() {
        return userType;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Long getBranchId() {
        return branchId;
    }

    public DataScope getDataScope() {
        return dataScope;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}

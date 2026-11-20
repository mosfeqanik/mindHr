package com.mindhr.service;

import com.mindhr.dto.UserRequest;
import com.mindhr.dto.UserResponse;
import com.mindhr.entity.Organization;
import com.mindhr.entity.Role;
import com.mindhr.entity.User;
import com.mindhr.entity.enums.UserType;
import com.mindhr.exception.DuplicateResourceException;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.UserMapper;
import com.mindhr.repository.OrganizationRepository;
import com.mindhr.repository.RoleRepository;
import com.mindhr.repository.UserRepository;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Users are managed by the Super Admin or an Admin. Super Admin accounts cannot be created, and only a
 * Super Admin may edit or delete one, preventing privilege escalation. An "org
 * account" is scoped like any other {@code Tenant} via {@link TenantContext}, even though
 * {@code User} itself carries a nullable organization rather than extending it.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /** Platform staff only: support needs the list to assign tickets; organization users have no access. */
    public List<UserResponse> list(Long requestedOrganizationId) {
        TenantContext.requirePlatformStaff();
        if (TenantContext.isPlatformStaff()) {
            List<User> users = requestedOrganizationId != null
                    ? userRepository.findByOrganizationId(requestedOrganizationId)
                    : userRepository.findAll();
            return users.stream().map(UserMapper::toResponse).toList();
        }
        Long ownOrganizationId = TenantContext.resolveOrganizationId(null);
        return userRepository.findByOrganizationId(ownOrganizationId).stream().map(UserMapper::toResponse).toList();
    }

    /** Platform staff read any user; an organization user only their own record. */
    public UserResponse getById(Long id) {
        if (!TenantContext.isPlatformStaff() && !id.equals(TenantContext.currentPrincipal().userId())) {
            throw new AccessDeniedException("Organization users can only read their own user record");
        }
        return UserMapper.toResponse(findAccessibleEntity(id));
    }

    /**
     * The Super Admin or an Admin creates Admin, mindHR Staff and Org User accounts and sets their passwords.
     * Super Admin accounts cannot be added: only the seeded one(s) exist.
     */
    @Transactional
    public UserResponse create(UserRequest request) {
        TenantContext.requireUserManager();
        rejectOrgAccountWithPlatformType(request);
        if (request.userType() == UserType.SUPER_ADMIN) {
            throw new IllegalArgumentException("Super Admin accounts cannot be added");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("A user with email '" + request.email() + "' already exists");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        boolean platformAccount = isPlatformAccount(request.organizationId(), request.userType());
        User user = new User();

        if (platformAccount) {
            user.setOrganization(null);
        } else {
            Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
            Organization organization = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
            user.setOrganization(organization);
        }

        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setUserType(request.userType());
        user.setActive(request.active() == null || request.active());
        user.setRoles(resolveRoles(request.roleIds(), user.getOrganization(), request.userType()));

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        TenantContext.requireUserManager();
        rejectOrgAccountWithPlatformType(request);
        User user = findAccessibleEntity(id);
        guardSuperAdminAccount(user);
        // A Super Admin stays a Super Admin, and nobody else can be turned into one.
        boolean wasSuperAdmin = user.getUserType() == UserType.SUPER_ADMIN;
        if (wasSuperAdmin != (request.userType() == UserType.SUPER_ADMIN)) {
            throw new IllegalArgumentException(wasSuperAdmin
                    ? "A Super Admin's user type cannot be changed"
                    : "A user cannot be changed into a Super Admin");
        }

        boolean becomesPlatformAccount = isPlatformAccount(request.organizationId(), request.userType());
        if (becomesPlatformAccount) {
            user.setOrganization(null);
        } else {
            Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
            Organization organization = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
            user.setOrganization(organization);
        }

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new DuplicateResourceException("A user with email '" + request.email() + "' already exists");
        }

        user.setEmail(request.email());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setUserType(request.userType());
        if (request.active() != null) {
            user.setActive(request.active());
        }
        if (request.roleIds() != null) {
            user.setRoles(resolveRoles(request.roleIds(), user.getOrganization(), request.userType()));
        } else if (!user.getRoles().stream().allMatch(r -> roleFits(r, user.getOrganization()))) {
            // Moving a user between organizations (or to/from the platform) drops roles that no longer apply.
            user.getRoles().removeIf(r -> !roleFits(r, user.getOrganization()));
        }

        return UserMapper.toResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        TenantContext.requireUserManager();
        User user = findAccessibleEntity(id);
        guardSuperAdminAccount(user);
        if (user.getId().equals(TenantContext.currentPrincipal().userId())) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }
        userRepository.delete(user);
    }

    /** Platform accounts (Super Admin, Admin, mindHR Staff) never belong to an organization; an org account is ORG_USER. */
    private static void rejectOrgAccountWithPlatformType(UserRequest request) {
        if (request.organizationId() != null && request.userType() != UserType.ORG_USER) {
            throw new IllegalArgumentException("A user that belongs to an organization must have user type ORG_USER");
        }
    }

    private boolean isPlatformAccount(Long organizationId, UserType userType) {
        return organizationId == null && (userType == UserType.SUPER_ADMIN || userType == UserType.ADMIN
                || userType == UserType.SAAS_STAFF);
    }

    /** Only a Super Admin may edit (incl. the password) or delete a Super Admin account; an Admin may not. */
    private void guardSuperAdminAccount(User user) {
        if (user.getUserType() == UserType.SUPER_ADMIN) {
            TenantContext.requireSuperAdmin();
        }
    }

    /**
     * Roles must match the user's side: an organization user only gets that organization's roles, a platform
     * user only platform roles (no organization). This stops, e.g., the SUPER_ADMIN role landing on an org user.
     */
    private Set<Role> resolveRoles(Set<Long> roleIds, Organization organization, UserType userType) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new HashSet<>();
        }
        if (roleIds.size() > 1) {
            throw new IllegalArgumentException("A user can have only one role");
        }
        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new ResourceNotFoundException("One or more roles were not found");
        }
        for (Role role : roles) {
            // hasRole('SUPER_ADMIN') unlocks plans, permissions and system settings: only for Super Admin users.
            if (role.getOrganization() == null && "SUPER_ADMIN".equalsIgnoreCase(role.getName())
                    && userType != UserType.SUPER_ADMIN) {
                throw new IllegalArgumentException("The SUPER_ADMIN role can only be given to a Super Admin user");
            }
            if (!roleFits(role, organization)) {
                throw new IllegalArgumentException(organization == null
                        ? "Role " + role.getName() + " belongs to an organization and cannot be given to a platform user"
                        : "Role " + role.getName() + " does not belong to " + organization.getName());
            }
        }
        return new HashSet<>(roles);
    }

    private static boolean roleFits(Role role, Organization organization) {
        Long roleOrgId = role.getOrganization() != null ? role.getOrganization().getId() : null;
        Long userOrgId = organization != null ? organization.getId() : null;
        return java.util.Objects.equals(roleOrgId, userOrgId);
    }

    private User findAccessibleEntity(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        Long entityOrganizationId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        if (!TenantContext.canAccess(entityOrganizationId)) {
            throw new ResourceNotFoundException("User not found: " + id);
        }
        return user;
    }
}

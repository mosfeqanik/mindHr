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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A "platform account" (organizationId null, or userType SUPER_ADMIN/SAAS_STAFF) may only be
 * created or modified by an existing Super Admin, preventing privilege escalation. An "org
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

    public UserResponse getById(Long id) {
        TenantContext.requirePlatformStaff();
        return UserMapper.toResponse(findAccessibleEntity(id));
    }

    /** Only the Super Admin creates users of any type (Super Admin, SaaS Staff, Org User) and sets passwords. */
    @Transactional
    public UserResponse create(UserRequest request) {
        TenantContext.requireSuperAdmin();
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("A user with email '" + request.email() + "' already exists");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        boolean platformAccount = isPlatformAccount(request.organizationId(), request.userType());
        User user = new User();

        if (platformAccount) {
            TenantContext.requireSuperAdmin();
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
        TenantContext.requireSuperAdmin();
        User user = findAccessibleEntity(id);
        guardIfPlatformAccount(user);

        boolean becomesPlatformAccount = isPlatformAccount(request.organizationId(), request.userType());
        if (becomesPlatformAccount) {
            TenantContext.requireSuperAdmin();
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
        TenantContext.requireSuperAdmin();
        User user = findAccessibleEntity(id);
        guardIfPlatformAccount(user);
        userRepository.delete(user);
    }

    private boolean isPlatformAccount(Long organizationId, UserType userType) {
        return organizationId == null && (userType == UserType.SUPER_ADMIN || userType == UserType.SAAS_STAFF);
    }

    /** A platform account may only be touched by a Super Admin, even one that is otherwise "accessible". */
    private void guardIfPlatformAccount(User user) {
        if (user.getOrganization() == null || user.getUserType() != com.mindhr.entity.enums.UserType.ORG_USER) {
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

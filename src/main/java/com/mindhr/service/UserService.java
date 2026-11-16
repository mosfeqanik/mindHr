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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Users are managed by SaaS platform staff only. Organization users cannot list, create, change
 * or delete users (not even in their own organization); they may only read their own record.
 * Without this, any employee could send {@code PUT /api/users/{ownId}} with a new
 * {@code userType} or {@code roleIds} and take over the platform.
 *
 * <p>Rules that also bind platform staff:
 * <ul>
 *   <li>A "platform account" (SUPER_ADMIN / SAAS_STAFF, no organization) may only be created or
 *       changed by a Super Admin.</li>
 *   <li>An account that belongs to an organization is always {@code ORG_USER}.</li>
 *   <li>Roles must match the account: an org account gets roles of its own organization only,
 *       a platform account gets platform roles only.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponse> list(Long requestedOrganizationId) {
        TenantContext.requirePlatformStaff();
        List<User> users = requestedOrganizationId != null
                ? userRepository.findByOrganizationId(requestedOrganizationId)
                : userRepository.findAll();
        return users.stream().map(UserMapper::toResponse).toList();
    }

    /** Platform staff read any user; an organization user only their own record. */
    public UserResponse getById(Long id) {
        if (!TenantContext.isPlatformStaff() && !id.equals(TenantContext.currentPrincipal().userId())) {
            throw new AccessDeniedException("Organization users can only read their own user record");
        }
        return UserMapper.toResponse(findAccessibleEntity(id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        TenantContext.requirePlatformStaff();
        rejectOrgAccountWithPlatformType(request);
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
        user.setRoles(resolveRoles(request.roleIds(), user.getOrganization()));

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        TenantContext.requirePlatformStaff();
        rejectOrgAccountWithPlatformType(request);
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
            user.setRoles(resolveRoles(request.roleIds(), user.getOrganization()));
        } else if (!rolesMatch(user.getRoles(), user.getOrganization())) {
            // The account moved to another organization (or to/from platform): its old roles no longer apply.
            user.setRoles(new HashSet<>());
        }

        return UserMapper.toResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        TenantContext.requirePlatformStaff();
        if (id.equals(TenantContext.currentPrincipal().userId())) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }
        User user = findAccessibleEntity(id);
        guardIfPlatformAccount(user);
        userRepository.delete(user);
    }

    /** SUPER_ADMIN / SAAS_STAFF accounts never belong to an organization; an org account is always ORG_USER. */
    private static void rejectOrgAccountWithPlatformType(UserRequest request) {
        if (request.organizationId() != null && request.userType() != UserType.ORG_USER) {
            throw new IllegalArgumentException("A user that belongs to an organization must have user type ORG_USER");
        }
    }

    private boolean isPlatformAccount(Long organizationId, UserType userType) {
        return organizationId == null && (userType == UserType.SUPER_ADMIN || userType == UserType.SAAS_STAFF);
    }

    /** A platform account may only be touched by a Super Admin, even one that is otherwise "accessible". */
    private void guardIfPlatformAccount(User user) {
        if (user.getOrganization() == null || user.getUserType() != UserType.ORG_USER) {
            TenantContext.requireSuperAdmin();
        }
    }

    /**
     * Roles must belong to the account's side: an org account gets roles of its own organization,
     * a platform account (organization null) gets platform roles. Otherwise an org user could be
     * given the platform SUPER_ADMIN role or another organization's roles.
     */
    private Set<Role> resolveRoles(Set<Long> roleIds, Organization organization) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new ResourceNotFoundException("One or more roles were not found");
        }
        if (!rolesMatch(roles, organization)) {
            throw new IllegalArgumentException(organization == null
                    ? "A platform account can only have platform roles"
                    : "Roles must belong to the user's organization");
        }
        return new HashSet<>(roles);
    }

    private static boolean rolesMatch(Collection<Role> roles, Organization organization) {
        Long organizationId = organization != null ? organization.getId() : null;
        return roles.stream().allMatch(role -> Objects.equals(
                role.getOrganization() != null ? role.getOrganization().getId() : null, organizationId));
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

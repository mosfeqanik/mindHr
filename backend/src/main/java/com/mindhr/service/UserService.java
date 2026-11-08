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

    public List<UserResponse> list(Long requestedOrganizationId) {
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
        return UserMapper.toResponse(findAccessibleEntity(id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
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
        user.setRoles(resolveRoles(request.roleIds()));

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
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
            user.setRoles(resolveRoles(request.roleIds()));
        }

        return UserMapper.toResponse(user);
    }

    @Transactional
    public void delete(Long id) {
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

    private Set<Role> resolveRoles(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new ResourceNotFoundException("One or more roles were not found");
        }
        return new HashSet<>(roles);
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

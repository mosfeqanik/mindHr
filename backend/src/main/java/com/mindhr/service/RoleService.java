package com.mindhr.service;

import com.mindhr.dto.RoleRequest;
import com.mindhr.dto.RoleResponse;
import com.mindhr.entity.Organization;
import com.mindhr.entity.Permission;
import com.mindhr.entity.Role;
import com.mindhr.entity.enums.RoleScope;
import com.mindhr.exception.DuplicateResourceException;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.RoleMapper;
import com.mindhr.repository.OrganizationRepository;
import com.mindhr.repository.PermissionRepository;
import com.mindhr.repository.RoleRepository;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A platform (built-in) role has {@code organizationId == null} and may only be managed by the
 * Super Admin; an org custom role is scoped like a {@code Tenant} via
 * {@link TenantContext}. Every {@code ORG_USER} can also read platform roles, since those are
 * the built-ins available to assign within their own organization. {@code system} roles (the
 * seeded Super Admin role) can never be edited or deleted through the API.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final PermissionRepository permissionRepository;

    public List<RoleResponse> list(Long requestedOrganizationId) {
        List<Role> roles;
        if (TenantContext.isPlatformStaff()) {
            roles = requestedOrganizationId != null
                    ? roleRepository.findByOrganizationIdOrOrganizationIdIsNull(requestedOrganizationId)
                    : roleRepository.findByOrganizationIdIsNull();
        } else {
            Long ownOrganizationId = TenantContext.resolveOrganizationId(null);
            roles = roleRepository.findByOrganizationIdOrOrganizationIdIsNull(ownOrganizationId);
        }
        return roles.stream().map(RoleMapper::toResponse).toList();
    }

    public RoleResponse getById(Long id) {
        return RoleMapper.toResponse(findAccessibleEntity(id));
    }

    @Transactional
    public RoleResponse create(RoleRequest request) {
        Role role = new Role();

        if (request.organizationId() == null) {
            TenantContext.requireSuperAdmin();
            if (roleRepository.existsByOrganizationIdIsNullAndNameIgnoreCase(request.name())) {
                throw new DuplicateResourceException("A platform role named '" + request.name() + "' already exists");
            }
            role.setOrganization(null);
            role.setScope(RoleScope.PLATFORM);
        } else {
            Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
            if (roleRepository.existsByOrganizationIdAndNameIgnoreCase(organizationId, request.name())) {
                throw new DuplicateResourceException("A role named '" + request.name() + "' already exists for this organization");
            }
            Organization organization = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
            role.setOrganization(organization);
            role.setScope(RoleScope.ORGANIZATION);
        }

        role.setName(request.name());
        role.setDescription(request.description());
        role.setDataScope(request.dataScope());
        role.setSystem(false);
        role.setPermissions(resolvePermissions(request.permissionIds()));

        return RoleMapper.toResponse(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse update(Long id, RoleRequest request) {
        Role role = findAccessibleEntity(id);
        rejectIfSystem(role);

        boolean movingToPlatform = request.organizationId() == null;
        if (movingToPlatform) {
            TenantContext.requireSuperAdmin();
            if (roleRepository.existsByOrganizationIdIsNullAndNameIgnoreCaseAndIdNot(request.name(), id)) {
                throw new DuplicateResourceException("A platform role named '" + request.name() + "' already exists");
            }
            role.setOrganization(null);
            role.setScope(RoleScope.PLATFORM);
        } else {
            Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
            if (roleRepository.existsByOrganizationIdAndNameIgnoreCaseAndIdNot(organizationId, request.name(), id)) {
                throw new DuplicateResourceException("A role named '" + request.name() + "' already exists for this organization");
            }
            Organization organization = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
            role.setOrganization(organization);
            role.setScope(RoleScope.ORGANIZATION);
        }

        role.setName(request.name());
        role.setDescription(request.description());
        if (request.dataScope() != null) {
            role.setDataScope(request.dataScope());
        }
        if (request.permissionIds() != null) {
            role.setPermissions(resolvePermissions(request.permissionIds()));
        }

        return RoleMapper.toResponse(role);
    }

    @Transactional
    public void delete(Long id) {
        Role role = findAccessibleEntity(id);
        rejectIfSystem(role);
        roleRepository.delete(role);
    }

    private void rejectIfSystem(Role role) {
        if (role.isSystem()) {
            throw new IllegalArgumentException("System roles cannot be modified or deleted");
        }
    }

    private Set<Permission> resolvePermissions(Set<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw new ResourceNotFoundException("One or more permissions were not found");
        }
        return new HashSet<>(permissions);
    }

    private Role findAccessibleEntity(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
        Long entityOrganizationId = role.getOrganization() != null ? role.getOrganization().getId() : null;
        if (entityOrganizationId != null && !TenantContext.canAccess(entityOrganizationId)) {
            throw new ResourceNotFoundException("Role not found: " + id);
        }
        return role;
    }
}

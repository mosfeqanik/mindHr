package com.mindhr.service;

import com.mindhr.dto.*;
import com.mindhr.entity.*;
import com.mindhr.entity.enums.*;
import com.mindhr.exception.*;
import com.mindhr.mapper.*;
import com.mindhr.repository.*;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchService {

    private final EmployeeRepository employeeRepository;
    private final BranchRepository repository;
    private final OrganizationRepository organizationRepository;
    private final PlanLimitService planLimitService;

    public List<BranchResponse> list(Long requestedOrganizationId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        return repository.findByOrganizationId(organizationId).stream()
                .map(BranchMapper::toResponse)
                .toList();
    }

    public BranchResponse getById(Long id) {
        return BranchMapper.toResponse(findAccessibleEntity(id));
    }

    @Transactional
    public BranchResponse create(BranchRequest request) {
        Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        planLimitService.assertCanAddBranch(organizationId, repository.countByOrganizationId(organizationId));
        Branch entity = new Branch();
        entity.setOrganization(organization);
        applyRequest(entity, request, organizationId);
        Branch saved = repository.save(entity);
        return BranchMapper.toResponse(saved);
    }

    @Transactional
    public BranchResponse update(Long id, BranchRequest request) {
        Branch entity = findAccessibleEntity(id);
        applyRequest(entity, request, entity.getOrganization().getId());
        return BranchMapper.toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findAccessibleEntity(id));
    }

    private void applyRequest(Branch entity, BranchRequest request, Long organizationId) {
        entity.setName(request.name());
        entity.setCode(request.code());
        entity.setAddress(request.address());
        entity.setPhone(request.phone());
        entity.setManager(resolveEmployee(request.managerId(), organizationId, false));
        entity.setActive(request.active() == null ? true : request.active());
        entity.setLatitude(request.latitude());
        entity.setLongitude(request.longitude());
        entity.setGeofenceRadiusM(request.geofenceRadiusM());
    }

    private Employee resolveEmployee(Long id, Long organizationId, boolean required) {
        if (id == null) {
            if (required) {
                throw new IllegalArgumentException("Employee is required");
            }
            return null;
        }
        Employee ref = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
        if (!ref.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("Employee not found: " + id);
        }
        return ref;
    }

    private Branch findAccessibleEntity(Long id) {
        Branch entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + id));
        if (!TenantContext.canAccess(entity.getOrganization().getId())) {
            throw new ResourceNotFoundException("Branch not found: " + id);
        }
        return entity;
    }
}

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
public class DepartmentService {

    private final BranchRepository branchRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository repository;
    private final OrganizationRepository organizationRepository;

    public List<DepartmentResponse> list(Long requestedOrganizationId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        return repository.findByOrganizationId(organizationId).stream()
                .map(DepartmentMapper::toResponse)
                .toList();
    }

    public DepartmentResponse getById(Long id) {
        return DepartmentMapper.toResponse(findAccessibleEntity(id));
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        Department entity = new Department();
        entity.setOrganization(organization);
        applyRequest(entity, request, organizationId);
        Department saved = repository.save(entity);
        return DepartmentMapper.toResponse(saved);
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department entity = findAccessibleEntity(id);
        applyRequest(entity, request, entity.getOrganization().getId());
        return DepartmentMapper.toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findAccessibleEntity(id));
    }

    private void applyRequest(Department entity, DepartmentRequest request, Long organizationId) {
        entity.setName(request.name());
        entity.setBranch(resolveBranch(request.branchId(), organizationId, false));
        entity.setHead(resolveEmployee(request.headId(), organizationId, false));
        entity.setActive(request.active() == null ? true : request.active());
    }

    private Branch resolveBranch(Long id, Long organizationId, boolean required) {
        if (id == null) {
            if (required) {
                throw new IllegalArgumentException("Branch is required");
            }
            return null;
        }
        Branch ref = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + id));
        if (!ref.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("Branch not found: " + id);
        }
        return ref;
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

    private Department findAccessibleEntity(Long id) {
        Department entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
        if (!TenantContext.canAccess(entity.getOrganization().getId())) {
            throw new ResourceNotFoundException("Department not found: " + id);
        }
        return entity;
    }
}

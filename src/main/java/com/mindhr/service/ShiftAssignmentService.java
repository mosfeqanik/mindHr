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
public class ShiftAssignmentService {

    private final ShiftRepository shiftRepository;
    private final ShiftAssignmentRepository repository;
    private final OrganizationRepository organizationRepository;
    private final DataScopeService scope;

    public List<ShiftAssignmentResponse> list(Long requestedOrganizationId, Long employeeId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        Set<Long> visible = scope.visibleEmployeeIds(organizationId);
        return repository.findByOrganizationId(organizationId).stream()
                .filter(e -> visible == null || visible.contains(e.getEmployee().getId()))
                .filter(e -> employeeId == null || employeeId.equals(e.getEmployee().getId()))
                .map(ShiftAssignmentMapper::toResponse)
                .toList();
    }

    public ShiftAssignmentResponse getById(Long id) {
        return ShiftAssignmentMapper.toResponse(findAccessibleEntity(id));
    }

    @Transactional
    public ShiftAssignmentResponse create(ShiftAssignmentRequest request) {
        Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        ShiftAssignment entity = new ShiftAssignment();
        entity.setOrganization(organization);
        applyRequest(entity, request, organizationId);
        ShiftAssignment saved = repository.save(entity);
        return ShiftAssignmentMapper.toResponse(saved);
    }

    @Transactional
    public ShiftAssignmentResponse update(Long id, ShiftAssignmentRequest request) {
        ShiftAssignment entity = findAccessibleEntity(id);
        applyRequest(entity, request, entity.getOrganization().getId());
        return ShiftAssignmentMapper.toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findAccessibleEntity(id));
    }

    private void applyRequest(ShiftAssignment entity, ShiftAssignmentRequest request, Long organizationId) {
        entity.setEmployee(scope.resolveEmployeeForWrite(request.employeeId(), organizationId));
        entity.setShift(resolveShift(request.shiftId(), organizationId, false));
        entity.setWorkDate(request.workDate());
    }

    private Shift resolveShift(Long id, Long organizationId, boolean required) {
        if (id == null) {
            if (required) {
                throw new IllegalArgumentException("Shift is required");
            }
            return null;
        }
        Shift ref = shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found: " + id));
        if (!ref.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("Shift not found: " + id);
        }
        return ref;
    }

    private ShiftAssignment findAccessibleEntity(Long id) {
        ShiftAssignment entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShiftAssignment not found: " + id));
        if (!TenantContext.canAccess(entity.getOrganization().getId())) {
            throw new ResourceNotFoundException("ShiftAssignment not found: " + id);
        }
        scope.assertCanAccess(entity.getEmployee());
        return entity;
    }
}

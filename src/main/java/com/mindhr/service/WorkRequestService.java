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
public class WorkRequestService {

    private final EmployeeRepository employeeRepository;
    private final WorkRequestRepository repository;
    private final OrganizationRepository organizationRepository;
    private final DataScopeService scope;

    public List<WorkRequestResponse> list(Long requestedOrganizationId, Long employeeId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        Set<Long> visible = scope.visibleEmployeeIds(organizationId);
        return repository.findByOrganizationId(organizationId).stream()
                .filter(e -> visible == null || visible.contains(e.getEmployee().getId()))
                .filter(e -> employeeId == null || employeeId.equals(e.getEmployee().getId()))
                .map(WorkRequestMapper::toResponse)
                .toList();
    }

    public WorkRequestResponse getById(Long id) {
        return WorkRequestMapper.toResponse(findAccessibleEntity(id));
    }

    private void applyRequest(WorkRequest entity, WorkRequestRequest request, Long organizationId) {
        entity.setEmployee(scope.resolveEmployeeForWrite(request.employeeId(), organizationId));
        entity.setType(request.type());
        entity.setTitle(request.title());
        entity.setDetails(request.details());
        entity.setAmount(request.amount());
        entity.setRequestDate(request.requestDate());
        entity.setStatus(request.status() == null ? RequestStatus.PENDING : request.status());
        entity.setDecidedBy(resolveEmployee(request.decidedById(), organizationId, false));
        entity.setDecidedAt(request.decidedAt());
        entity.setDecisionNote(request.decisionNote());
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

    private WorkRequest findAccessibleEntity(Long id) {
        WorkRequest entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkRequest not found: " + id));
        if (!TenantContext.canAccess(entity.getOrganization().getId())) {
            throw new ResourceNotFoundException("WorkRequest not found: " + id);
        }
        scope.assertCanAccess(entity.getEmployee());
        return entity;
    }
}

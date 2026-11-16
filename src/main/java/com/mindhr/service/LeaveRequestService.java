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
public class LeaveRequestService {

    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveRequestRepository repository;
    private final OrganizationRepository organizationRepository;
    private final DataScopeService scope;

    public List<LeaveRequestResponse> list(Long requestedOrganizationId, Long employeeId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        Set<Long> visible = scope.visibleEmployeeIds(organizationId);
        return repository.findByOrganizationId(organizationId).stream()
                .filter(e -> visible == null || visible.contains(e.getEmployee().getId()))
                .filter(e -> employeeId == null || employeeId.equals(e.getEmployee().getId()))
                .map(LeaveRequestMapper::toResponse)
                .toList();
    }

    public LeaveRequestResponse getById(Long id) {
        return LeaveRequestMapper.toResponse(findAccessibleEntity(id));
    }

    private void applyRequest(LeaveRequest entity, LeaveRequestRequest request, Long organizationId) {
        entity.setEmployee(scope.resolveEmployeeForWrite(request.employeeId(), organizationId));
        entity.setLeaveType(resolveLeaveType(request.leaveTypeId(), organizationId, true));
        entity.setFromDate(request.fromDate());
        entity.setToDate(request.toDate());
        entity.setDays(request.days());
        entity.setReason(request.reason());
        entity.setStatus(request.status() == null ? LeaveStatus.PENDING : request.status());
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

    private LeaveType resolveLeaveType(Long id, Long organizationId, boolean required) {
        if (id == null) {
            if (required) {
                throw new IllegalArgumentException("LeaveType is required");
            }
            return null;
        }
        LeaveType ref = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveType not found: " + id));
        if (!ref.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("LeaveType not found: " + id);
        }
        return ref;
    }

    private LeaveRequest findAccessibleEntity(Long id) {
        LeaveRequest entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest not found: " + id));
        if (!TenantContext.canAccess(entity.getOrganization().getId())) {
            throw new ResourceNotFoundException("LeaveRequest not found: " + id);
        }
        scope.assertCanAccess(entity.getEmployee());
        return entity;
    }
}

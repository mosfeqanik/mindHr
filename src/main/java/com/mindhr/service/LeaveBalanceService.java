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
public class LeaveBalanceService {

    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository repository;
    private final OrganizationRepository organizationRepository;
    private final DataScopeService scope;

    public List<LeaveBalanceResponse> list(Long requestedOrganizationId, Long employeeId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        Set<Long> visible = scope.visibleEmployeeIds(organizationId);
        return repository.findByOrganizationId(organizationId).stream()
                .filter(e -> visible == null || visible.contains(e.getEmployee().getId()))
                .filter(e -> employeeId == null || employeeId.equals(e.getEmployee().getId()))
                .map(LeaveBalanceMapper::toResponse)
                .toList();
    }

    public LeaveBalanceResponse getById(Long id) {
        return LeaveBalanceMapper.toResponse(findAccessibleEntity(id));
    }

    @Transactional
    public LeaveBalanceResponse create(LeaveBalanceRequest request) {
        Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        LeaveBalance entity = new LeaveBalance();
        entity.setOrganization(organization);
        applyRequest(entity, request, organizationId);
        LeaveBalance saved = repository.save(entity);
        return LeaveBalanceMapper.toResponse(saved);
    }

    @Transactional
    public LeaveBalanceResponse update(Long id, LeaveBalanceRequest request) {
        LeaveBalance entity = findAccessibleEntity(id);
        applyRequest(entity, request, entity.getOrganization().getId());
        return LeaveBalanceMapper.toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findAccessibleEntity(id));
    }

    private void applyRequest(LeaveBalance entity, LeaveBalanceRequest request, Long organizationId) {
        entity.setEmployee(scope.resolveEmployeeForWrite(request.employeeId(), organizationId));
        entity.setLeaveType(resolveLeaveType(request.leaveTypeId(), organizationId, true));
        entity.setYear(request.year() == null ? 0 : request.year());
        entity.setEntitledDays(request.entitledDays());
        entity.setUsedDays(request.usedDays() == null ? BigDecimal.ZERO : request.usedDays());
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

    private LeaveBalance findAccessibleEntity(Long id) {
        LeaveBalance entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveBalance not found: " + id));
        if (!TenantContext.canAccess(entity.getOrganization().getId())) {
            throw new ResourceNotFoundException("LeaveBalance not found: " + id);
        }
        scope.assertCanAccess(entity.getEmployee());
        return entity;
    }
}

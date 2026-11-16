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
public class EmployeeEventService {

    private final EmployeeEventRepository repository;
    private final OrganizationRepository organizationRepository;
    private final DataScopeService scope;

    public List<EmployeeEventResponse> list(Long requestedOrganizationId, Long employeeId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        Set<Long> visible = scope.visibleEmployeeIds(organizationId);
        return repository.findByOrganizationId(organizationId).stream()
                .filter(e -> visible == null || visible.contains(e.getEmployee().getId()))
                .filter(e -> employeeId == null || employeeId.equals(e.getEmployee().getId()))
                .map(EmployeeEventMapper::toResponse)
                .toList();
    }

    public EmployeeEventResponse getById(Long id) {
        return EmployeeEventMapper.toResponse(findAccessibleEntity(id));
    }

    @Transactional
    public EmployeeEventResponse create(EmployeeEventRequest request) {
        Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        EmployeeEvent entity = new EmployeeEvent();
        entity.setOrganization(organization);
        applyRequest(entity, request, organizationId);
        EmployeeEvent saved = repository.save(entity);
        return EmployeeEventMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findAccessibleEntity(id));
    }

    private void applyRequest(EmployeeEvent entity, EmployeeEventRequest request, Long organizationId) {
        entity.setEmployee(scope.resolveEmployeeForWrite(request.employeeId(), organizationId));
        entity.setType(request.type());
        entity.setEventDate(request.eventDate());
        entity.setTitle(request.title());
        entity.setDetails(request.details());
        entity.setOldValue(request.oldValue());
        entity.setNewValue(request.newValue());
    }

    private EmployeeEvent findAccessibleEntity(Long id) {
        EmployeeEvent entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeEvent not found: " + id));
        if (!TenantContext.canAccess(entity.getOrganization().getId())) {
            throw new ResourceNotFoundException("EmployeeEvent not found: " + id);
        }
        scope.assertCanAccess(entity.getEmployee());
        return entity;
    }
}

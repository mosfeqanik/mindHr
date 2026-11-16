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
public class LeaveTypeService {

    private final LeaveTypeRepository repository;
    private final OrganizationRepository organizationRepository;

    public List<LeaveTypeResponse> list(Long requestedOrganizationId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        return repository.findByOrganizationId(organizationId).stream()
                .map(LeaveTypeMapper::toResponse)
                .toList();
    }

    public LeaveTypeResponse getById(Long id) {
        return LeaveTypeMapper.toResponse(findAccessibleEntity(id));
    }

    @Transactional
    public LeaveTypeResponse create(LeaveTypeRequest request) {
        Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        LeaveType entity = new LeaveType();
        entity.setOrganization(organization);
        applyRequest(entity, request, organizationId);
        LeaveType saved = repository.save(entity);
        return LeaveTypeMapper.toResponse(saved);
    }

    @Transactional
    public LeaveTypeResponse update(Long id, LeaveTypeRequest request) {
        LeaveType entity = findAccessibleEntity(id);
        applyRequest(entity, request, entity.getOrganization().getId());
        return LeaveTypeMapper.toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findAccessibleEntity(id));
    }

    private void applyRequest(LeaveType entity, LeaveTypeRequest request, Long organizationId) {
        entity.setName(request.name());
        entity.setAnnualDays(request.annualDays() == null ? 0 : request.annualDays());
        entity.setPaid(request.paid() == null ? true : request.paid());
        entity.setActive(request.active() == null ? true : request.active());
    }

    private LeaveType findAccessibleEntity(Long id) {
        LeaveType entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveType not found: " + id));
        if (!TenantContext.canAccess(entity.getOrganization().getId())) {
            throw new ResourceNotFoundException("LeaveType not found: " + id);
        }
        return entity;
    }
}

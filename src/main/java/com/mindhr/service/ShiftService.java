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
public class ShiftService {

    private final ShiftRepository repository;
    private final OrganizationRepository organizationRepository;

    public List<ShiftResponse> list(Long requestedOrganizationId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        return repository.findByOrganizationId(organizationId).stream()
                .map(ShiftMapper::toResponse)
                .toList();
    }

    public ShiftResponse getById(Long id) {
        return ShiftMapper.toResponse(findAccessibleEntity(id));
    }

    @Transactional
    public ShiftResponse create(ShiftRequest request) {
        Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        Shift entity = new Shift();
        entity.setOrganization(organization);
        applyRequest(entity, request, organizationId);
        Shift saved = repository.save(entity);
        return ShiftMapper.toResponse(saved);
    }

    @Transactional
    public ShiftResponse update(Long id, ShiftRequest request) {
        Shift entity = findAccessibleEntity(id);
        applyRequest(entity, request, entity.getOrganization().getId());
        return ShiftMapper.toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findAccessibleEntity(id));
    }

    private void applyRequest(Shift entity, ShiftRequest request, Long organizationId) {
        entity.setName(request.name());
        entity.setStartTime(request.startTime());
        entity.setEndTime(request.endTime());
        entity.setActive(request.active() == null ? true : request.active());
    }

    private Shift findAccessibleEntity(Long id) {
        Shift entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found: " + id));
        if (!TenantContext.canAccess(entity.getOrganization().getId())) {
            throw new ResourceNotFoundException("Shift not found: " + id);
        }
        return entity;
    }
}

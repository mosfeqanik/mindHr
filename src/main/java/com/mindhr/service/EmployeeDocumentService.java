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
public class EmployeeDocumentService {

    private final EmployeeDocumentRepository repository;
    private final OrganizationRepository organizationRepository;
    private final DataScopeService scope;

    public List<EmployeeDocumentResponse> list(Long requestedOrganizationId, Long employeeId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        Set<Long> visible = scope.visibleEmployeeIds(organizationId);
        return repository.findByOrganizationId(organizationId).stream()
                .filter(e -> visible == null || visible.contains(e.getEmployee().getId()))
                .filter(e -> employeeId == null || employeeId.equals(e.getEmployee().getId()))
                .map(EmployeeDocumentMapper::toResponse)
                .toList();
    }

    public EmployeeDocumentResponse getById(Long id) {
        return EmployeeDocumentMapper.toResponse(findAccessibleEntity(id));
    }

    @Transactional
    public EmployeeDocumentResponse create(EmployeeDocumentRequest request) {
        Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        EmployeeDocument entity = new EmployeeDocument();
        entity.setOrganization(organization);
        applyRequest(entity, request, organizationId);
        EmployeeDocument saved = repository.save(entity);
        return EmployeeDocumentMapper.toResponse(saved);
    }

    @Transactional
    public EmployeeDocumentResponse update(Long id, EmployeeDocumentRequest request) {
        EmployeeDocument entity = findAccessibleEntity(id);
        applyRequest(entity, request, entity.getOrganization().getId());
        return EmployeeDocumentMapper.toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findAccessibleEntity(id));
    }

    private void applyRequest(EmployeeDocument entity, EmployeeDocumentRequest request, Long organizationId) {
        entity.setEmployee(scope.resolveEmployeeForWrite(request.employeeId(), organizationId));
        entity.setType(request.type());
        entity.setTitle(request.title());
        entity.setFileUrl(request.fileUrl());
        entity.setIssuedAt(request.issuedAt());
        entity.setNote(request.note());
    }

    private EmployeeDocument findAccessibleEntity(Long id) {
        EmployeeDocument entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeDocument not found: " + id));
        if (!TenantContext.canAccess(entity.getOrganization().getId())) {
            throw new ResourceNotFoundException("EmployeeDocument not found: " + id);
        }
        scope.assertCanAccess(entity.getEmployee());
        return entity;
    }
}

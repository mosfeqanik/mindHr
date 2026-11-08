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
public class SalaryStructureService {

    private final SalaryStructureRepository repository;
    private final OrganizationRepository organizationRepository;
    private final DataScopeService scope;
    private final EmployeeTimelineService eventService;

    public List<SalaryStructureResponse> list(Long requestedOrganizationId, Long employeeId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        Set<Long> visible = scope.visibleEmployeeIds(organizationId);
        return repository.findByOrganizationId(organizationId).stream()
                .filter(e -> visible == null || visible.contains(e.getEmployee().getId()))
                .filter(e -> employeeId == null || employeeId.equals(e.getEmployee().getId()))
                .map(SalaryStructureMapper::toResponse)
                .toList();
    }

    public SalaryStructureResponse getById(Long id) {
        return SalaryStructureMapper.toResponse(findAccessibleEntity(id));
    }

    @Transactional
    public SalaryStructureResponse create(SalaryStructureRequest request) {
        Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        SalaryStructure entity = new SalaryStructure();
        entity.setOrganization(organization);
        applyRequest(entity, request, organizationId);
        SalaryStructure saved = repository.save(entity);
        eventService.record(saved.getEmployee(), EmployeeEventType.SALARY_REVISED, "Salary set effective " + saved.getEffectiveFrom(), null, saved.getBasicSalary().toPlainString(), null);
        return SalaryStructureMapper.toResponse(saved);
    }

    @Transactional
    public SalaryStructureResponse update(Long id, SalaryStructureRequest request) {
        SalaryStructure entity = findAccessibleEntity(id);
        applyRequest(entity, request, entity.getOrganization().getId());
        return SalaryStructureMapper.toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findAccessibleEntity(id));
    }

    private void applyRequest(SalaryStructure entity, SalaryStructureRequest request, Long organizationId) {
        entity.setEmployee(scope.resolveEmployeeForWrite(request.employeeId(), organizationId));
        entity.setEffectiveFrom(request.effectiveFrom());
        entity.setBasicSalary(request.basicSalary());
        entity.setAllowances(request.allowances() == null ? BigDecimal.ZERO : request.allowances());
        entity.setCurrency(request.currency() == null ? "BDT" : request.currency());
    }

    private SalaryStructure findAccessibleEntity(Long id) {
        SalaryStructure entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalaryStructure not found: " + id));
        if (!TenantContext.canAccess(entity.getOrganization().getId())) {
            throw new ResourceNotFoundException("SalaryStructure not found: " + id);
        }
        scope.assertCanAccess(entity.getEmployee());
        return entity;
    }
}

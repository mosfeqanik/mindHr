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
public class EmployeePaymentAccountService {

    private final EmployeePaymentAccountRepository repository;
    private final OrganizationRepository organizationRepository;
    private final DataScopeService scope;

    public List<EmployeePaymentAccountResponse> list(Long requestedOrganizationId, Long employeeId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        Set<Long> visible = scope.visibleEmployeeIds(organizationId);
        return repository.findByOrganizationId(organizationId).stream()
                .filter(e -> visible == null || visible.contains(e.getEmployee().getId()))
                .filter(e -> employeeId == null || employeeId.equals(e.getEmployee().getId()))
                .map(EmployeePaymentAccountMapper::toResponse)
                .toList();
    }

    public EmployeePaymentAccountResponse getById(Long id) {
        return EmployeePaymentAccountMapper.toResponse(findAccessibleEntity(id));
    }

    @Transactional
    public EmployeePaymentAccountResponse create(EmployeePaymentAccountRequest request) {
        Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        EmployeePaymentAccount entity = new EmployeePaymentAccount();
        entity.setOrganization(organization);
        applyRequest(entity, request, organizationId);
        EmployeePaymentAccount saved = repository.save(entity);
        return EmployeePaymentAccountMapper.toResponse(saved);
    }

    @Transactional
    public EmployeePaymentAccountResponse update(Long id, EmployeePaymentAccountRequest request) {
        EmployeePaymentAccount entity = findAccessibleEntity(id);
        applyRequest(entity, request, entity.getOrganization().getId());
        return EmployeePaymentAccountMapper.toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findAccessibleEntity(id));
    }

    private void applyRequest(EmployeePaymentAccount entity, EmployeePaymentAccountRequest request, Long organizationId) {
        entity.setEmployee(scope.resolveEmployeeForWrite(request.employeeId(), organizationId));
        entity.setType(request.type());
        entity.setProvider(request.provider());
        entity.setAccountName(request.accountName());
        entity.setAccountNumber(request.accountNumber());
        entity.setPrimaryAccount(request.primaryAccount() == null ? false : request.primaryAccount());
    }

    private EmployeePaymentAccount findAccessibleEntity(Long id) {
        EmployeePaymentAccount entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeePaymentAccount not found: " + id));
        if (!TenantContext.canAccess(entity.getOrganization().getId())) {
            throw new ResourceNotFoundException("EmployeePaymentAccount not found: " + id);
        }
        scope.assertCanAccess(entity.getEmployee());
        return entity;
    }
}

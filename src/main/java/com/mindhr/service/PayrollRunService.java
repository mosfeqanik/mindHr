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
public class PayrollRunService {

    private final BranchRepository branchRepository;
    private final PayrollRunRepository repository;
    private final OrganizationRepository organizationRepository;

    public List<PayrollRunResponse> list(Long requestedOrganizationId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        return repository.findByOrganizationId(organizationId).stream()
                .map(PayrollRunMapper::toResponse)
                .toList();
    }

    public PayrollRunResponse getById(Long id) {
        return PayrollRunMapper.toResponse(findAccessibleEntity(id));
    }

    private void applyRequest(PayrollRun entity, PayrollRunRequest request, Long organizationId) {
        entity.setYear(request.year() == null ? 0 : request.year());
        entity.setMonth(request.month() == null ? 0 : request.month());
        entity.setBranch(resolveBranch(request.branchId(), organizationId, false));
        entity.setStatus(request.status() == null ? PayrollStatus.DRAFT : request.status());
        entity.setNote(request.note());
    }

    private Branch resolveBranch(Long id, Long organizationId, boolean required) {
        if (id == null) {
            if (required) {
                throw new IllegalArgumentException("Branch is required");
            }
            return null;
        }
        Branch ref = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + id));
        if (!ref.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("Branch not found: " + id);
        }
        return ref;
    }

    private PayrollRun findAccessibleEntity(Long id) {
        PayrollRun entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRun not found: " + id));
        if (!TenantContext.canAccess(entity.getOrganization().getId())) {
            throw new ResourceNotFoundException("PayrollRun not found: " + id);
        }
        return entity;
    }
}

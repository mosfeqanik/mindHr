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
public class AssetAssignmentService {

    private final AssetRepository assetRepository;
    private final AssetAssignmentRepository repository;
    private final OrganizationRepository organizationRepository;
    private final DataScopeService scope;

    public List<AssetAssignmentResponse> list(Long requestedOrganizationId, Long employeeId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        Set<Long> visible = scope.visibleEmployeeIds(organizationId);
        return repository.findByOrganizationId(organizationId).stream()
                .filter(e -> visible == null || visible.contains(e.getEmployee().getId()))
                .filter(e -> employeeId == null || employeeId.equals(e.getEmployee().getId()))
                .map(AssetAssignmentMapper::toResponse)
                .toList();
    }

    public AssetAssignmentResponse getById(Long id) {
        return AssetAssignmentMapper.toResponse(findAccessibleEntity(id));
    }

    private void applyRequest(AssetAssignment entity, AssetAssignmentRequest request, Long organizationId) {
        entity.setAsset(resolveAsset(request.assetId(), organizationId, true));
        entity.setEmployee(scope.resolveEmployeeForWrite(request.employeeId(), organizationId));
        entity.setAssignedAt(request.assignedAt());
        entity.setReturnedAt(request.returnedAt());
        entity.setConditionNote(request.conditionNote());
    }

    private Asset resolveAsset(Long id, Long organizationId, boolean required) {
        if (id == null) {
            if (required) {
                throw new IllegalArgumentException("Asset is required");
            }
            return null;
        }
        Asset ref = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + id));
        if (!ref.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("Asset not found: " + id);
        }
        return ref;
    }

    private AssetAssignment findAccessibleEntity(Long id) {
        AssetAssignment entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AssetAssignment not found: " + id));
        if (!TenantContext.canAccess(entity.getOrganization().getId())) {
            throw new ResourceNotFoundException("AssetAssignment not found: " + id);
        }
        scope.assertCanAccess(entity.getEmployee());
        return entity;
    }
}

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
public class AssetService {

    private final AssetRepository repository;
    private final OrganizationRepository organizationRepository;

    public List<AssetResponse> list(Long requestedOrganizationId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        return repository.findByOrganizationId(organizationId).stream()
                .map(AssetMapper::toResponse)
                .toList();
    }

    public AssetResponse getById(Long id) {
        return AssetMapper.toResponse(findAccessibleEntity(id));
    }

    @Transactional
    public AssetResponse create(AssetRequest request) {
        Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        Asset entity = new Asset();
        entity.setOrganization(organization);
        applyRequest(entity, request, organizationId);
        Asset saved = repository.save(entity);
        return AssetMapper.toResponse(saved);
    }

    @Transactional
    public AssetResponse update(Long id, AssetRequest request) {
        Asset entity = findAccessibleEntity(id);
        applyRequest(entity, request, entity.getOrganization().getId());
        return AssetMapper.toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findAccessibleEntity(id));
    }

    private void applyRequest(Asset entity, AssetRequest request, Long organizationId) {
        entity.setName(request.name());
        entity.setAssetTag(request.assetTag());
        entity.setSerialNumber(request.serialNumber());
        entity.setCategory(request.category());
        entity.setDescription(request.description());
        entity.setStatus(request.status() == null ? AssetStatus.AVAILABLE : request.status());
    }

    private Asset findAccessibleEntity(Long id) {
        Asset entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + id));
        if (!TenantContext.canAccess(entity.getOrganization().getId())) {
            throw new ResourceNotFoundException("Asset not found: " + id);
        }
        return entity;
    }
}

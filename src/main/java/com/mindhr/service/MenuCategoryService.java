package com.mindhr.service;

import com.mindhr.dto.MenuCategoryRequest;
import com.mindhr.dto.MenuCategoryResponse;
import com.mindhr.entity.MenuCategory;
import com.mindhr.entity.Organization;
import com.mindhr.exception.DuplicateResourceException;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.MenuCategoryMapper;
import com.mindhr.repository.MenuCategoryRepository;
import com.mindhr.repository.OrganizationRepository;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuCategoryService {

    private final MenuCategoryRepository menuCategoryRepository;
    private final OrganizationRepository organizationRepository;

    public List<MenuCategoryResponse> list(Long requestedOrganizationId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        return menuCategoryRepository.findByOrganizationIdOrderBySortOrderAsc(organizationId).stream()
                .map(MenuCategoryMapper::toResponse)
                .toList();
    }

    public MenuCategoryResponse getById(Long id) {
        return MenuCategoryMapper.toResponse(findAccessibleEntity(id));
    }

    @Transactional
    public MenuCategoryResponse create(MenuCategoryRequest request) {
        Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());

        if (menuCategoryRepository.existsByOrganizationIdAndNameIgnoreCase(organizationId, request.name())) {
            throw new DuplicateResourceException("A menu category named '" + request.name() + "' already exists");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));

        MenuCategory category = new MenuCategory();
        category.setOrganization(organization);
        applyRequest(category, request);
        return MenuCategoryMapper.toResponse(menuCategoryRepository.save(category));
    }

    @Transactional
    public MenuCategoryResponse update(Long id, MenuCategoryRequest request) {
        MenuCategory category = findAccessibleEntity(id);

        if (menuCategoryRepository.existsByOrganizationIdAndNameIgnoreCaseAndIdNot(
                category.getOrganization().getId(), request.name(), id)) {
            throw new DuplicateResourceException("A menu category named '" + request.name() + "' already exists");
        }

        applyRequest(category, request);
        return MenuCategoryMapper.toResponse(category);
    }

    @Transactional
    public void delete(Long id) {
        MenuCategory category = findAccessibleEntity(id);
        menuCategoryRepository.delete(category);
    }

    private void applyRequest(MenuCategory category, MenuCategoryRequest request) {
        category.setName(request.name());
        category.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        category.setActive(request.active() == null || request.active());
    }

    private MenuCategory findAccessibleEntity(Long id) {
        MenuCategory category = menuCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found: " + id));
        if (!TenantContext.canAccess(category.getOrganization().getId())) {
            throw new ResourceNotFoundException("Menu category not found: " + id);
        }
        return category;
    }
}

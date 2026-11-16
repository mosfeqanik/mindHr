package com.mindhr.service;

import com.mindhr.dto.MenuItemRequest;
import com.mindhr.dto.MenuItemResponse;
import com.mindhr.entity.MenuCategory;
import com.mindhr.entity.MenuItem;
import com.mindhr.entity.Organization;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.MenuItemMapper;
import com.mindhr.repository.MenuCategoryRepository;
import com.mindhr.repository.MenuItemRepository;
import com.mindhr.repository.OrganizationRepository;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final OrganizationRepository organizationRepository;

    public List<MenuItemResponse> list(Long requestedOrganizationId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        return menuItemRepository.findByOrganizationId(organizationId).stream()
                .map(MenuItemMapper::toResponse)
                .toList();
    }

    public MenuItemResponse getById(Long id) {
        return MenuItemMapper.toResponse(findAccessibleEntity(id));
    }

    @Transactional
    public MenuItemResponse create(MenuItemRequest request) {
        Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        MenuCategory category = resolveCategory(request.categoryId(), organizationId);

        MenuItem item = new MenuItem();
        item.setOrganization(organization);
        item.setCategory(category);
        applyRequest(item, request);
        return MenuItemMapper.toResponse(menuItemRepository.save(item));
    }

    @Transactional
    public MenuItemResponse update(Long id, MenuItemRequest request) {
        MenuItem item = findAccessibleEntity(id);
        item.setCategory(resolveCategory(request.categoryId(), item.getOrganization().getId()));
        applyRequest(item, request);
        return MenuItemMapper.toResponse(item);
    }

    @Transactional
    public void delete(Long id) {
        MenuItem item = findAccessibleEntity(id);
        menuItemRepository.delete(item);
    }

    private void applyRequest(MenuItem item, MenuItemRequest request) {
        item.setName(request.name());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setImageUrl(request.imageUrl());
        item.setAvailable(request.available() == null || request.available());
    }

    private MenuCategory resolveCategory(Long categoryId, Long organizationId) {
        MenuCategory category = menuCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found: " + categoryId));
        if (!category.getOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Menu category does not belong to this organization");
        }
        return category;
    }

    private MenuItem findAccessibleEntity(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + id));
        if (!TenantContext.canAccess(item.getOrganization().getId())) {
            throw new ResourceNotFoundException("Menu item not found: " + id);
        }
        return item;
    }
}

package com.mindhr.mapper;

import com.mindhr.dto.MenuCategoryResponse;
import com.mindhr.entity.MenuCategory;

public final class MenuCategoryMapper {

    private MenuCategoryMapper() {
    }

    public static MenuCategoryResponse toResponse(MenuCategory category) {
        return new MenuCategoryResponse(
                category.getId(),
                category.getOrganization().getId(),
                category.getName(),
                category.getSortOrder(),
                category.isActive()
        );
    }
}

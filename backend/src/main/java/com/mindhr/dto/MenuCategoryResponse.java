package com.mindhr.dto;

public record MenuCategoryResponse(
        Long id,
        Long organizationId,
        String name,
        int sortOrder,
        boolean active
) {
}

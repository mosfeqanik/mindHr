package com.mindhr.dto;

import java.math.BigDecimal;

public record MenuItemResponse(
        Long id,
        Long organizationId,
        Long categoryId,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        boolean available
) {
}

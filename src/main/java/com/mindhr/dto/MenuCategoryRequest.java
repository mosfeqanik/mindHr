package com.mindhr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record MenuCategoryRequest(

        Long organizationId,

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @PositiveOrZero(message = "Sort order must not be negative")
        Integer sortOrder,

        Boolean active
) {
}

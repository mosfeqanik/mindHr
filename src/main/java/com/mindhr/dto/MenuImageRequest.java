package com.mindhr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record MenuImageRequest(

        /** Only read for platform staff, who must operate on an explicit organization; ignored for an org user. */
        Long organizationId,

        @NotBlank(message = "Image URL is required")
        @Size(max = 500, message = "Image URL must be at most 500 characters")
        String imageUrl,

        @Size(max = 200, message = "Caption must be at most 200 characters")
        String caption,

        @PositiveOrZero(message = "Sort order must not be negative")
        Integer sortOrder
) {
}

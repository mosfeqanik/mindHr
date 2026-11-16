package com.mindhr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SystemSettingRequest(

        @NotBlank(message = "Key is required")
        @Size(max = 100, message = "Key must be at most 100 characters")
        String key,

        String value,

        @Size(max = 255, message = "Description must be at most 255 characters")
        String description
) {
}

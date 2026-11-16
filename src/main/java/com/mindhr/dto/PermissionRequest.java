package com.mindhr.dto;

import com.mindhr.entity.enums.PermissionAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PermissionRequest(

        @NotBlank(message = "Module is required")
        @Size(max = 50, message = "Module must be at most 50 characters")
        String module,

        @NotNull(message = "Action is required")
        PermissionAction action
) {
}

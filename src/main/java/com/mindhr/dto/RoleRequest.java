package com.mindhr.dto;

import com.mindhr.entity.enums.DataScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record RoleRequest(

        /** Null creates/edits a platform (built-in) role, restricted to the Super Admin; set for an org custom role. */
        Long organizationId,

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Size(max = 255, message = "Description must be at most 255 characters")
        String description,

        /** Optional; defaults to ORGANIZATION for org roles. */
        DataScope dataScope,

        Set<Long> permissionIds
) {
}

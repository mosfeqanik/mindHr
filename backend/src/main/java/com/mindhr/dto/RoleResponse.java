package com.mindhr.dto;

import com.mindhr.entity.enums.DataScope;
import com.mindhr.entity.enums.RoleScope;

import java.util.Set;

public record RoleResponse(
        Long id,
        Long organizationId,
        String name,
        String description,
        RoleScope scope,
        DataScope dataScope,
        boolean system,
        Set<PermissionResponse> permissions
) {
}

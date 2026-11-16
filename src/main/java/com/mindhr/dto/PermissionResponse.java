package com.mindhr.dto;

import com.mindhr.entity.enums.PermissionAction;

public record PermissionResponse(
        Long id,
        String module,
        PermissionAction action
) {
}

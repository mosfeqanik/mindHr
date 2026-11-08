package com.mindhr.mapper;

import com.mindhr.dto.PermissionResponse;
import com.mindhr.entity.Permission;

public final class PermissionMapper {

    private PermissionMapper() {
    }

    public static PermissionResponse toResponse(Permission permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getModule(),
                permission.getAction()
        );
    }
}

package com.mindhr.mapper;

import com.mindhr.dto.DepartmentResponse;
import com.mindhr.entity.Department;

public final class DepartmentMapper {

    private DepartmentMapper() {
    }

    public static DepartmentResponse toResponse(Department entity) {
        return new DepartmentResponse(
                entity.getId(),
                entity.getOrganization().getId(),
                entity.getName(),
                entity.getBranch() != null ? entity.getBranch().getId() : null,
                entity.getHead() != null ? entity.getHead().getId() : null,
                entity.isActive()
        );
    }
}

package com.mindhr.mapper;

import com.mindhr.dto.BranchResponse;
import com.mindhr.entity.Branch;

public final class BranchMapper {

    private BranchMapper() {
    }

    public static BranchResponse toResponse(Branch entity) {
        return new BranchResponse(
                entity.getId(),
                entity.getOrganization().getId(),
                entity.getName(),
                entity.getCode(),
                entity.getAddress(),
                entity.getPhone(),
                entity.getManager() != null ? entity.getManager().getId() : null,
                entity.isActive(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getGeofenceRadiusM()
        );
    }
}

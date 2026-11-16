package com.mindhr.dto;

import com.mindhr.entity.enums.*;

import java.math.BigDecimal;
import java.time.*;

public record BranchResponse(
        Long id,
        Long organizationId,
        String name,
        String code,
        String address,
        String phone,
        Long managerId,
        boolean active,
        Double latitude,
        Double longitude,
        Integer geofenceRadiusM
) {
}

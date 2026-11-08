package com.mindhr.dto;

import com.mindhr.entity.enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.*;

public record BranchRequest(

        Long organizationId,

        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @Size(max = 30, message = "Code must be at most 30 characters")
        String code,

        @Size(max = 255, message = "Address must be at most 255 characters")
        String address,

        @Size(max = 30, message = "Phone must be at most 30 characters")
        String phone,

        Long managerId,

        Boolean active,

        Double latitude,

        Double longitude,

        Integer geofenceRadiusM
) {
}

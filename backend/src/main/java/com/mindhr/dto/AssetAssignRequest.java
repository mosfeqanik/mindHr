package com.mindhr.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AssetAssignRequest(

        Long organizationId,

        @NotNull(message = "Asset is required")
        Long assetId,

        @NotNull(message = "Employee is required")
        Long employeeId,

        /** Defaults to today. */
        LocalDate assignedAt
) {
}

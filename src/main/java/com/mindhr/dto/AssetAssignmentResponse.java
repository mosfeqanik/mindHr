package com.mindhr.dto;

import com.mindhr.entity.enums.*;

import java.math.BigDecimal;
import java.time.*;

public record AssetAssignmentResponse(
        Long id,
        Long organizationId,
        Long assetId,
        Long employeeId,
        LocalDate assignedAt,
        LocalDate returnedAt,
        String conditionNote
) {
}

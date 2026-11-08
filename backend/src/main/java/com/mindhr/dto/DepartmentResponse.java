package com.mindhr.dto;

import com.mindhr.entity.enums.*;

import java.math.BigDecimal;
import java.time.*;

public record DepartmentResponse(
        Long id,
        Long organizationId,
        String name,
        Long branchId,
        Long headId,
        boolean active
) {
}

package com.mindhr.dto;

import com.mindhr.entity.enums.*;

import java.math.BigDecimal;
import java.time.*;

public record LeaveTypeResponse(
        Long id,
        Long organizationId,
        String name,
        int annualDays,
        boolean paid,
        boolean active
) {
}

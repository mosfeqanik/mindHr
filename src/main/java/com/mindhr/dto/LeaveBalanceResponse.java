package com.mindhr.dto;

import com.mindhr.entity.enums.*;

import java.math.BigDecimal;
import java.time.*;

public record LeaveBalanceResponse(
        Long id,
        Long organizationId,
        Long employeeId,
        Long leaveTypeId,
        int year,
        BigDecimal entitledDays,
        BigDecimal usedDays
) {
}

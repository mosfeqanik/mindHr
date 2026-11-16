package com.mindhr.dto;

import com.mindhr.entity.enums.*;

import java.math.BigDecimal;
import java.time.*;

public record AttendanceResponse(
        Long id,
        Long organizationId,
        Long employeeId,
        LocalDate workDate,
        LocalDateTime checkInAt,
        LocalDateTime checkOutAt,
        Double checkInLatitude,
        Double checkInLongitude,
        Double checkOutLatitude,
        Double checkOutLongitude,
        AttendanceMethod method,
        AttendanceStatus status,
        int lateMinutes,
        int workedMinutes,
        String note
) {
}

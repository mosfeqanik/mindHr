package com.mindhr.dto;

import com.mindhr.entity.enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.*;

public record AttendanceRequest(

        Long organizationId,

        @NotNull(message = "Employee id is required")
        Long employeeId,

        @NotNull(message = "Work date is required")
        LocalDate workDate,

        LocalDateTime checkInAt,

        LocalDateTime checkOutAt,

        Double checkInLatitude,

        Double checkInLongitude,

        Double checkOutLatitude,

        Double checkOutLongitude,

        AttendanceMethod method,

        AttendanceStatus status,

        Integer lateMinutes,

        Integer workedMinutes,

        @Size(max = 500, message = "Note must be at most 500 characters")
        String note
) {
}

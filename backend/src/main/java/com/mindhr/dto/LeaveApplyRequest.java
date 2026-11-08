package com.mindhr.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record LeaveApplyRequest(

        @NotNull(message = "Leave type is required")
        Long leaveTypeId,

        @NotNull(message = "From date is required")
        LocalDate fromDate,

        @NotNull(message = "To date is required")
        LocalDate toDate,

        @Size(max = 500, message = "Reason must be at most 500 characters")
        String reason
) {
}

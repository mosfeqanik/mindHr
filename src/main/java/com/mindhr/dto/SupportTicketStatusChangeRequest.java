package com.mindhr.dto;

import com.mindhr.entity.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record SupportTicketStatusChangeRequest(
        @NotNull(message = "Status is required")
        TicketStatus status
) {
}

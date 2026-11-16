package com.mindhr.dto;

import com.mindhr.entity.enums.TicketPriority;
import com.mindhr.entity.enums.TicketStatus;
import com.mindhr.entity.enums.TicketType;

import java.time.LocalDateTime;

public record SupportTicketResponse(
        Long id,
        Long organizationId,
        Long createdByUserId,
        String createdByName,
        Long assignedToUserId,
        String assignedToName,
        String ticketNumber,
        String subject,
        String description,
        TicketType type,
        TicketStatus status,
        TicketPriority priority,
        LocalDateTime resolvedAt,
        LocalDateTime closedAt
) {
}

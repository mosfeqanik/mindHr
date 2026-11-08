package com.mindhr.mapper;

import com.mindhr.dto.SupportTicketResponse;
import com.mindhr.entity.SupportTicket;

public final class SupportTicketMapper {

    private SupportTicketMapper() {
    }

    public static SupportTicketResponse toResponse(SupportTicket ticket) {
        return new SupportTicketResponse(
                ticket.getId(),
                ticket.getOrganization().getId(),
                ticket.getCreatedBy().getId(),
                ticket.getCreatedBy().getFullName(),
                ticket.getAssignedTo() != null ? ticket.getAssignedTo().getId() : null,
                ticket.getAssignedTo() != null ? ticket.getAssignedTo().getFullName() : null,
                ticket.getTicketNumber(),
                ticket.getSubject(),
                ticket.getDescription(),
                ticket.getType(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getResolvedAt(),
                ticket.getClosedAt()
        );
    }
}

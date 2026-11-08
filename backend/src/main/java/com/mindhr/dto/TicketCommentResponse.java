package com.mindhr.dto;

public record TicketCommentResponse(
        Long id,
        Long ticketId,
        Long authorId,
        String authorName,
        String body,
        boolean internal
) {
}

package com.mindhr.dto;

import jakarta.validation.constraints.NotBlank;

public record TicketCommentRequest(

        @NotBlank(message = "Body is required")
        String body,

        /** Only platform staff may post an internal (staff-only) comment. */
        Boolean internal
) {
}

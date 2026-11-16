package com.mindhr.dto;

import jakarta.validation.constraints.NotNull;

/** Links an already-created {@code Organization} back to the {@code Lead} it came from and marks it CONVERTED. */
public record LeadConvertRequest(
        @NotNull(message = "organizationId is required")
        Long organizationId
) {
}

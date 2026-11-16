package com.mindhr.dto;

import jakarta.validation.constraints.Size;

/** Optional note attached when approving or rejecting a leave or work request. */
public record DecisionRequest(
        @Size(max = 500, message = "Note must be at most 500 characters")
        String note
) {
}

package com.mindhr.dto;

import com.mindhr.entity.enums.ContractStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContractStatusChangeRequest(
        @NotNull(message = "Status is required")
        ContractStatus status,

        @Size(max = 500, message = "Note must be at most 500 characters")
        String note
) {
}

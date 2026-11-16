package com.mindhr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractRequest(

        Long organizationId,

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 150, message = "Counterparty name must be at most 150 characters")
        String counterpartyName,

        LocalDate startDate,

        LocalDate endDate,

        BigDecimal value,

        @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
        String currency,

        String terms
) {
}

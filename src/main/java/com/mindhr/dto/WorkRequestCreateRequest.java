package com.mindhr.dto;

import com.mindhr.entity.enums.RequestType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WorkRequestCreateRequest(

        @NotNull(message = "Type is required")
        RequestType type,

        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must be at most 150 characters")
        String title,

        String details,

        @DecimalMin(value = "0.0", message = "Amount must not be negative")
        BigDecimal amount,

        LocalDate requestDate
) {
}

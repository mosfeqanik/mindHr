package com.mindhr.dto;

import com.mindhr.entity.enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.*;

public record EmployeeDocumentRequest(

        Long organizationId,

        @NotNull(message = "Employee id is required")
        Long employeeId,

        @NotNull(message = "Type is required")
        DocumentType type,

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @NotBlank(message = "File url is required")
        @Size(max = 500, message = "File url must be at most 500 characters")
        String fileUrl,

        LocalDate issuedAt,

        @Size(max = 500, message = "Note must be at most 500 characters")
        String note
) {
}

package com.mindhr.dto;

import com.mindhr.entity.enums.*;

import java.math.BigDecimal;
import java.time.*;

public record EmployeeDocumentResponse(
        Long id,
        Long organizationId,
        Long employeeId,
        DocumentType type,
        String title,
        String fileUrl,
        LocalDate issuedAt,
        String note
) {
}

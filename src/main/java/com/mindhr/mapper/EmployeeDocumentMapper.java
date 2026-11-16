package com.mindhr.mapper;

import com.mindhr.dto.EmployeeDocumentResponse;
import com.mindhr.entity.EmployeeDocument;

public final class EmployeeDocumentMapper {

    private EmployeeDocumentMapper() {
    }

    public static EmployeeDocumentResponse toResponse(EmployeeDocument entity) {
        return new EmployeeDocumentResponse(
                entity.getId(),
                entity.getOrganization().getId(),
                entity.getEmployee().getId(),
                entity.getType(),
                entity.getTitle(),
                entity.getFileUrl(),
                entity.getIssuedAt(),
                entity.getNote()
        );
    }
}

package com.mindhr.mapper;

import com.mindhr.dto.ContractDocumentResponse;
import com.mindhr.entity.ContractDocument;

public final class ContractDocumentMapper {

    private ContractDocumentMapper() {
    }

    public static ContractDocumentResponse toResponse(ContractDocument document) {
        return new ContractDocumentResponse(
                document.getId(),
                document.getContract().getId(),
                document.getUploadedBy() != null ? document.getUploadedBy().getId() : null,
                document.getUploadedBy() != null ? document.getUploadedBy().getFullName() : null,
                document.getFileName(),
                document.getFileUrl(),
                document.getContentType(),
                document.getFileSize(),
                document.getUploadedAt()
        );
    }
}

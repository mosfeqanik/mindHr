package com.mindhr.dto;

import java.time.LocalDateTime;

public record ContractDocumentResponse(
        Long id,
        Long contractId,
        Long uploadedByUserId,
        String uploadedByName,
        String fileName,
        String fileUrl,
        String contentType,
        Long fileSize,
        LocalDateTime uploadedAt
) {
}

package com.mindhr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContractDocumentRequest(

        @NotBlank(message = "File name is required")
        @Size(max = 255, message = "File name must be at most 255 characters")
        String fileName,

        @NotBlank(message = "File URL is required")
        @Size(max = 500, message = "File URL must be at most 500 characters")
        String fileUrl,

        @Size(max = 100, message = "Content type must be at most 100 characters")
        String contentType,

        Long fileSize
) {
}

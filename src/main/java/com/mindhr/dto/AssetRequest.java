package com.mindhr.dto;

import com.mindhr.entity.enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.*;

public record AssetRequest(

        Long organizationId,

        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @NotBlank(message = "Asset tag is required")
        @Size(max = 100, message = "Asset tag must be at most 100 characters")
        String assetTag,

        @Size(max = 100, message = "Serial number must be at most 100 characters")
        String serialNumber,

        @Size(max = 100, message = "Category must be at most 100 characters")
        String category,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        AssetStatus status
) {
}

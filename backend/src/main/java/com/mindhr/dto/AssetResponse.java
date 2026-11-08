package com.mindhr.dto;

import com.mindhr.entity.enums.*;

import java.math.BigDecimal;
import java.time.*;

public record AssetResponse(
        Long id,
        Long organizationId,
        String name,
        String assetTag,
        String serialNumber,
        String category,
        String description,
        AssetStatus status
) {
}

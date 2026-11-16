package com.mindhr.dto;

import com.mindhr.entity.enums.AssetStatus;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AssetReturnRequest(

        /** Defaults to today. */
        LocalDate returnedAt,

        @Size(max = 500, message = "Condition note must be at most 500 characters")
        String conditionNote,

        /** Status of the asset after return: AVAILABLE (default), MAINTENANCE or RETIRED. */
        AssetStatus assetStatus
) {
}

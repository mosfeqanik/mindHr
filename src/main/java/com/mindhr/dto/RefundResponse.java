package com.mindhr.dto;

import com.mindhr.entity.enums.RefundStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundResponse(
        Long id,
        Long paymentId,
        Long organizationId,
        BigDecimal amount,
        String reason,
        RefundStatus status,
        Long processedByUserId,
        String processedByName,
        LocalDateTime processedAt
) {
}

package com.mindhr.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReceiptResponse(
        Long id,
        Long organizationId,
        Long paymentId,
        String receiptNumber,
        BigDecimal amount,
        LocalDateTime issuedAt
) {
}

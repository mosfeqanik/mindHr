package com.mindhr.dto;

import com.mindhr.entity.enums.PaymentChannel;
import com.mindhr.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long organizationId,
        Long invoiceId,
        Long paymentMethodId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        PaymentChannel channel,
        String gateway,
        String gatewayTransactionId,
        LocalDateTime paidAt,
        boolean hasReceipt
) {
}

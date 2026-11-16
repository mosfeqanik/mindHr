package com.mindhr.mapper;

import com.mindhr.dto.PaymentResponse;
import com.mindhr.entity.Payment;

public final class PaymentMapper {

    private PaymentMapper() {
    }

    public static PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrganization().getId(),
                payment.getInvoice().getId(),
                payment.getPaymentMethod() != null ? payment.getPaymentMethod().getId() : null,
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getGateway(),
                payment.getGatewayTransactionId(),
                payment.getPaidAt(),
                payment.getReceipt() != null
        );
    }
}

package com.mindhr.mapper;

import com.mindhr.dto.ReceiptResponse;
import com.mindhr.entity.Receipt;

public final class ReceiptMapper {

    private ReceiptMapper() {
    }

    public static ReceiptResponse toResponse(Receipt receipt) {
        return new ReceiptResponse(
                receipt.getId(),
                receipt.getOrganization().getId(),
                receipt.getPayment().getId(),
                receipt.getReceiptNumber(),
                receipt.getAmount(),
                receipt.getIssuedAt()
        );
    }
}

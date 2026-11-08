package com.mindhr.mapper;

import com.mindhr.dto.RefundResponse;
import com.mindhr.entity.Refund;

public final class RefundMapper {

    private RefundMapper() {
    }

    public static RefundResponse toResponse(Refund refund) {
        return new RefundResponse(
                refund.getId(),
                refund.getPayment().getId(),
                refund.getPayment().getOrganization().getId(),
                refund.getAmount(),
                refund.getReason(),
                refund.getStatus(),
                refund.getProcessedBy() != null ? refund.getProcessedBy().getId() : null,
                refund.getProcessedBy() != null ? refund.getProcessedBy().getFullName() : null,
                refund.getProcessedAt()
        );
    }
}

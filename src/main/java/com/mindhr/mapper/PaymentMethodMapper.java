package com.mindhr.mapper;

import com.mindhr.dto.PaymentMethodResponse;
import com.mindhr.entity.PaymentMethod;

public final class PaymentMethodMapper {

    private PaymentMethodMapper() {
    }

    /** {@code gatewayToken} is deliberately left out of the response — it is write-only. */
    public static PaymentMethodResponse toResponse(PaymentMethod paymentMethod) {
        return new PaymentMethodResponse(
                paymentMethod.getId(),
                paymentMethod.getOrganization().getId(),
                paymentMethod.getType(),
                paymentMethod.getMaskedDetails(),
                paymentMethod.isDefaultMethod()
        );
    }
}

package com.mindhr.dto;

import com.mindhr.entity.enums.PaymentMethodType;

public record PaymentMethodResponse(
        Long id,
        Long organizationId,
        PaymentMethodType type,
        String maskedDetails,
        boolean defaultMethod
) {
}

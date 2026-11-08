package com.mindhr.dto;

import com.mindhr.entity.enums.PaymentMethodType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PaymentMethodRequest(

        /** Only read for platform staff, who must operate on an explicit organization; ignored for an org user. */
        Long organizationId,

        @NotNull(message = "Type is required")
        PaymentMethodType type,

        @Size(max = 50, message = "Masked details must be at most 50 characters")
        String maskedDetails,

        @Size(max = 255, message = "Gateway token must be at most 255 characters")
        String gatewayToken,

        Boolean defaultMethod
) {
}

package com.mindhr.dto;

import com.mindhr.entity.enums.CollectionChannel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** One month of the subscription's current plan, received by platform staff. */
public record SubscriptionPaymentRequest(

        @NotNull(message = "Payment method is required")
        CollectionChannel method,

        /** bKash TrxID or card approval code. Required for BKASH. */
        @Size(max = 100, message = "Transaction id must be at most 100 characters")
        String transactionId,

        String notes
) {
}

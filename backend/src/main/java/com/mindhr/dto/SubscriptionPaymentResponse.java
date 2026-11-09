package com.mindhr.dto;

import java.time.LocalDate;

public record SubscriptionPaymentResponse(
        SubscriptionResponse subscription,
        InvoiceResponse invoice,
        PaymentResponse payment,
        ReceiptResponse receipt,
        LocalDate periodStart,
        LocalDate periodEnd
) {
}

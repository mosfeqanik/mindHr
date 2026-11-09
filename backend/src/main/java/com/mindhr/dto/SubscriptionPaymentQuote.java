package com.mindhr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** What collecting one month would charge, and which month it would pay for. */
public record SubscriptionPaymentQuote(
        Long subscriptionId,
        String planName,
        BigDecimal amount,
        String currency,
        LocalDate periodStart,
        LocalDate periodEnd
) {
}

package com.mindhr.dto;

import com.mindhr.entity.enums.*;

import java.math.BigDecimal;
import java.time.*;

public record EmployeePaymentAccountResponse(
        Long id,
        Long organizationId,
        Long employeeId,
        PaymentAccountType type,
        String provider,
        String accountName,
        String accountNumber,
        boolean primaryAccount
) {
}

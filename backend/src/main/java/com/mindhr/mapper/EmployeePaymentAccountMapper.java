package com.mindhr.mapper;

import com.mindhr.dto.EmployeePaymentAccountResponse;
import com.mindhr.entity.EmployeePaymentAccount;

public final class EmployeePaymentAccountMapper {

    private EmployeePaymentAccountMapper() {
    }

    public static EmployeePaymentAccountResponse toResponse(EmployeePaymentAccount entity) {
        return new EmployeePaymentAccountResponse(
                entity.getId(),
                entity.getOrganization().getId(),
                entity.getEmployee().getId(),
                entity.getType(),
                entity.getProvider(),
                entity.getAccountName(),
                entity.getAccountNumber(),
                entity.isPrimaryAccount()
        );
    }
}

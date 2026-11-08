package com.mindhr.mapper;

import com.mindhr.dto.PayslipResponse;
import com.mindhr.entity.Payslip;

public final class PayslipMapper {

    private PayslipMapper() {
    }

    public static PayslipResponse toResponse(Payslip entity) {
        return new PayslipResponse(
                entity.getId(),
                entity.getOrganization().getId(),
                entity.getPayrollRun().getId(),
                entity.getEmployee().getId(),
                entity.getBasicSalary(),
                entity.getAllowances(),
                entity.getOvertime(),
                entity.getBonus(),
                entity.getCommission(),
                entity.getGrossSalary(),
                entity.getLateDeduction(),
                entity.getAbsentDeduction(),
                entity.getLoanDeduction(),
                entity.getAdvanceDeduction(),
                entity.getNetSalary(),
                entity.getDaysPresent(),
                entity.getDaysAbsent(),
                entity.getDaysLate(),
                entity.getStatus()
        );
    }
}

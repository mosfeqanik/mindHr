package com.mindhr.mapper;

import com.mindhr.dto.SalaryStructureResponse;
import com.mindhr.entity.SalaryStructure;

public final class SalaryStructureMapper {

    private SalaryStructureMapper() {
    }

    public static SalaryStructureResponse toResponse(SalaryStructure entity) {
        return new SalaryStructureResponse(
                entity.getId(),
                entity.getOrganization().getId(),
                entity.getEmployee().getId(),
                entity.getEffectiveFrom(),
                entity.getBasicSalary(),
                entity.getAllowances(),
                entity.getCurrency()
        );
    }
}

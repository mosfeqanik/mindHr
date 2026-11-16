package com.mindhr.mapper;

import com.mindhr.dto.PayrollRunResponse;
import com.mindhr.entity.PayrollRun;

public final class PayrollRunMapper {

    private PayrollRunMapper() {
    }

    public static PayrollRunResponse toResponse(PayrollRun entity) {
        return new PayrollRunResponse(
                entity.getId(),
                entity.getOrganization().getId(),
                entity.getYear(),
                entity.getMonth(),
                entity.getBranch() != null ? entity.getBranch().getId() : null,
                entity.getStatus(),
                entity.getNote()
        );
    }
}

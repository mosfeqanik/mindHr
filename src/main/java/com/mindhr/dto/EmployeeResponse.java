package com.mindhr.dto;

import com.mindhr.entity.enums.EmploymentType;

import java.time.LocalDate;

public record EmployeeResponse(
        Long id,
        Long organizationId,
        Long userId,
        String employeeCode,
        String fullName,
        String email,
        String phone,
        String position,
        String nationalIdSerial,
        String photoUrl,
        String address,
        String emergencyContactName,
        String emergencyContactPhone,
        String emergencyContactRelation,
        Long branchId,
        Long departmentId,
        Long managerId,
        EmploymentType employmentType,
        LocalDate joinedAt,
        boolean active
) {
}

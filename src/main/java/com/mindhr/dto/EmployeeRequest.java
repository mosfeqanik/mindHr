package com.mindhr.dto;

import com.mindhr.entity.enums.EmploymentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EmployeeRequest(

        Long organizationId,

        /**
         * Existing login account to link. When null, a login is created from {@code email} and
         * {@code password} so the employee can use the mobile app.
         */
        Long userId,

        /** Initial password for the auto-created login (create only; ignored when userId is set). */
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        /** Generated (EMP-0001, ...) when blank. */
        @Size(max = 50, message = "Employee code must be at most 50 characters")
        String employeeCode,

        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name must be at most 150 characters")
        String fullName,

        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        @Size(max = 30, message = "Phone must be at most 30 characters")
        String phone,

        @Size(max = 100, message = "Position must be at most 100 characters")
        String position,

        @Size(max = 100, message = "National ID serial must be at most 100 characters")
        String nationalIdSerial,

        @Size(max = 500, message = "Photo URL must be at most 500 characters")
        String photoUrl,

        @Size(max = 255, message = "Address must be at most 255 characters")
        String address,

        @Size(max = 150, message = "Emergency contact name must be at most 150 characters")
        String emergencyContactName,

        @Size(max = 30, message = "Emergency contact phone must be at most 30 characters")
        String emergencyContactPhone,

        @Size(max = 50, message = "Emergency contact relation must be at most 50 characters")
        String emergencyContactRelation,

        Long branchId,

        Long departmentId,

        Long managerId,

        EmploymentType employmentType,

        LocalDate joinedAt,

        Boolean active
) {
}

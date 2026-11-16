package com.mindhr.dto;

import com.mindhr.entity.enums.LeadStage;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LeadRequest(

        Long assignedToUserId,

        @NotBlank(message = "Company name is required")
        @Size(max = 150, message = "Company name must be at most 150 characters")
        String companyName,

        @NotBlank(message = "Contact name is required")
        @Size(max = 150, message = "Contact name must be at most 150 characters")
        String contactName,

        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        @Size(max = 30, message = "Phone must be at most 30 characters")
        String phone,

        @NotNull(message = "Stage is required")
        LeadStage stage,

        @Size(max = 50, message = "Source must be at most 50 characters")
        String source,

        String notes
) {
}

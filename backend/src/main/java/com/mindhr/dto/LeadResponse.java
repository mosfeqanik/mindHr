package com.mindhr.dto;

import com.mindhr.entity.enums.LeadStage;

public record LeadResponse(
        Long id,
        Long assignedToUserId,
        String assignedToName,
        Long convertedOrganizationId,
        String companyName,
        String contactName,
        String email,
        String phone,
        LeadStage stage,
        String source,
        String notes
) {
}

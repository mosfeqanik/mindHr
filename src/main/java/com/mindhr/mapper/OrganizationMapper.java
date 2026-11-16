package com.mindhr.mapper;

import com.mindhr.dto.OrganizationResponse;
import com.mindhr.entity.Organization;
import com.mindhr.entity.User;

public final class OrganizationMapper {

    private OrganizationMapper() {
    }

    public static OrganizationResponse toResponse(Organization organization) {
        User owner = organization.getOwner();
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getStatus(),
                organization.getEmail(),
                organization.getPhone(),
                organization.getAddress(),
                organization.getTrialEndsAt(),
                organization.getActivatedAt(),
                owner != null ? owner.getId() : null,
                owner != null ? owner.getFullName() : null,
                owner != null ? owner.getEmail() : null
        );
    }
}

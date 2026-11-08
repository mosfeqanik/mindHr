package com.mindhr.mapper;

import com.mindhr.dto.OrganizationSettingResponse;
import com.mindhr.entity.OrganizationSetting;

public final class OrganizationSettingMapper {

    private OrganizationSettingMapper() {
    }

    public static OrganizationSettingResponse toResponse(OrganizationSetting setting) {
        return new OrganizationSettingResponse(
                setting.getId(),
                setting.getOrganization().getId(),
                setting.getKey(),
                setting.getValue()
        );
    }
}

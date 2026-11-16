package com.mindhr.mapper;

import com.mindhr.dto.SystemSettingResponse;
import com.mindhr.entity.SystemSetting;

public final class SystemSettingMapper {

    private SystemSettingMapper() {
    }

    public static SystemSettingResponse toResponse(SystemSetting setting) {
        return new SystemSettingResponse(
                setting.getId(),
                setting.getKey(),
                setting.getValue(),
                setting.getDescription()
        );
    }
}

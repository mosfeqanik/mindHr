package com.mindhr.mapper;

import com.mindhr.dto.AssetResponse;
import com.mindhr.entity.Asset;

public final class AssetMapper {

    private AssetMapper() {
    }

    public static AssetResponse toResponse(Asset entity) {
        return new AssetResponse(
                entity.getId(),
                entity.getOrganization().getId(),
                entity.getName(),
                entity.getAssetTag(),
                entity.getSerialNumber(),
                entity.getCategory(),
                entity.getDescription(),
                entity.getStatus()
        );
    }
}

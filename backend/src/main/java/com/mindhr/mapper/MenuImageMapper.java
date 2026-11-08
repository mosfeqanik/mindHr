package com.mindhr.mapper;

import com.mindhr.dto.MenuImageResponse;
import com.mindhr.entity.MenuImage;

public final class MenuImageMapper {

    private MenuImageMapper() {
    }

    public static MenuImageResponse toResponse(MenuImage menuImage) {
        return new MenuImageResponse(
                menuImage.getId(),
                menuImage.getOrganization().getId(),
                menuImage.getImageUrl(),
                menuImage.getCaption(),
                menuImage.getSortOrder()
        );
    }
}

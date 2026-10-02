package com.superfercho.catalog.application.dto;

import com.superfercho.catalog.domain.model.CategoryIcon;
import java.util.UUID;

public record UpdateCategoryCommand(UUID categoryId, String name, String description, String icon) {

    public UpdateCategoryCommand {
        icon = CategoryIcon.parse(icon).name();
    }
}

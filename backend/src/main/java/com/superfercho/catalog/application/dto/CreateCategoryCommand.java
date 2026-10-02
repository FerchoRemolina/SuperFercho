package com.superfercho.catalog.application.dto;

import com.superfercho.catalog.domain.model.CategoryIcon;

public record CreateCategoryCommand(String name, String description, String icon) {

    public CreateCategoryCommand {
        icon = CategoryIcon.parse(icon).name();
    }
}

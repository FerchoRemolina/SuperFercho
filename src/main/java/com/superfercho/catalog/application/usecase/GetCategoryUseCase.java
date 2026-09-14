package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.CategoryResult;
import com.superfercho.catalog.application.dto.GetCategoryCommand;
import com.superfercho.catalog.application.exception.CategoryNotFoundException;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.domain.model.Category;

public final class GetCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public GetCategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryResult execute(GetCategoryCommand command) {
        Category category = categoryRepository
                .findById(command.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException(command.categoryId()));
        if (command.view() == CatalogView.PUBLIC && !CatalogVisibility.isPubliclyVisible(category)) {
            throw new CategoryNotFoundException(command.categoryId());
        }
        return CategoryResult.from(category);
    }
}

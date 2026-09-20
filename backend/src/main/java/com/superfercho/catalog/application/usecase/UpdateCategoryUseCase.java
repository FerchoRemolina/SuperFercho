package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.CategoryResult;
import com.superfercho.catalog.application.dto.UpdateCategoryCommand;
import com.superfercho.catalog.application.exception.CategoryNotFoundException;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.domain.model.Category;
import java.time.Clock;

public final class UpdateCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final Clock clock;

    public UpdateCategoryUseCase(CategoryRepository categoryRepository, Clock clock) {
        this.categoryRepository = categoryRepository;
        this.clock = clock;
    }

    public CategoryResult execute(UpdateCategoryCommand command) {
        Category category = categoryRepository
                .findById(command.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException(command.categoryId()));
        Category updated =
                category.updateInformation(command.name(), command.description(), clock.instant());
        return CategoryResult.from(categoryRepository.save(updated));
    }
}

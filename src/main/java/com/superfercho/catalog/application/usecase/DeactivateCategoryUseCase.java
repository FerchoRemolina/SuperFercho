package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.CategoryResult;
import com.superfercho.catalog.application.dto.DeactivateCategoryCommand;
import com.superfercho.catalog.application.exception.CategoryNotFoundException;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.domain.model.Category;
import java.time.Clock;

public final class DeactivateCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final Clock clock;

    public DeactivateCategoryUseCase(CategoryRepository categoryRepository, Clock clock) {
        this.categoryRepository = categoryRepository;
        this.clock = clock;
    }

    public CategoryResult execute(DeactivateCategoryCommand command) {
        Category category = categoryRepository
                .findById(command.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException(command.categoryId()));
        return CategoryResult.from(categoryRepository.save(category.deactivate(clock.instant())));
    }
}

package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.ActivateCategoryCommand;
import com.superfercho.catalog.application.dto.CategoryResult;
import com.superfercho.catalog.application.exception.CategoryNotFoundException;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.domain.model.Category;
import java.time.Clock;

public final class ActivateCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final Clock clock;

    public ActivateCategoryUseCase(CategoryRepository categoryRepository, Clock clock) {
        this.categoryRepository = categoryRepository;
        this.clock = clock;
    }

    public CategoryResult execute(ActivateCategoryCommand command) {
        Category category = categoryRepository
                .findById(command.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException(command.categoryId()));
        return CategoryResult.from(categoryRepository.save(category.activate(clock.instant())));
    }
}

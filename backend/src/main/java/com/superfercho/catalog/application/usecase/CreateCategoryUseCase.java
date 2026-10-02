package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.CategoryResult;
import com.superfercho.catalog.application.dto.CreateCategoryCommand;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.CategoryIcon;
import com.superfercho.catalog.domain.model.CategoryStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public final class CreateCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final Clock clock;

    public CreateCategoryUseCase(CategoryRepository categoryRepository, Clock clock) {
        this.categoryRepository = categoryRepository;
        this.clock = clock;
    }

    public CategoryResult execute(CreateCategoryCommand command) {
        Instant now = clock.instant();
        Category category = Category.create(
                UUID.randomUUID(),
                command.name(),
                command.description(),
                CategoryIcon.valueOf(command.icon()),
                CategoryStatus.ACTIVE,
                now,
                now);
        return CategoryResult.from(categoryRepository.save(category));
    }
}

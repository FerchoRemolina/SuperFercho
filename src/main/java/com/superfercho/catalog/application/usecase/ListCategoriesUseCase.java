package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.CategoryResult;
import com.superfercho.catalog.application.dto.ListCategoriesCommand;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.CategoryStatus;
import java.util.List;

public final class ListCategoriesUseCase {

    private final CategoryRepository categoryRepository;

    public ListCategoriesUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResult> execute(ListCategoriesCommand command) {
        List<Category> categories = command.view() == CatalogView.PUBLIC
                ? categoryRepository.findByStatus(CategoryStatus.ACTIVE)
                : categoryRepository.findAll();
        return categories.stream().map(CategoryResult::from).toList();
    }
}

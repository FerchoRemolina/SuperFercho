package com.superfercho.catalog.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.dto.ActivateCategoryCommand;
import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.CategoryResult;
import com.superfercho.catalog.application.dto.CreateCategoryCommand;
import com.superfercho.catalog.application.dto.DeactivateCategoryCommand;
import com.superfercho.catalog.application.dto.GetCategoryCommand;
import com.superfercho.catalog.application.dto.ListCategoriesCommand;
import com.superfercho.catalog.application.dto.UpdateCategoryCommand;
import com.superfercho.catalog.application.exception.CategoryNotFoundException;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.CategoryStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");
    private static final UUID CATEGORY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private CategoryRepository categoryRepository;

    private CreateCategoryUseCase createCategory;
    private UpdateCategoryUseCase updateCategory;
    private GetCategoryUseCase getCategory;
    private ListCategoriesUseCase listCategories;
    private ActivateCategoryUseCase activateCategory;
    private DeactivateCategoryUseCase deactivateCategory;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        createCategory = new CreateCategoryUseCase(categoryRepository, clock);
        updateCategory = new UpdateCategoryUseCase(categoryRepository, clock);
        getCategory = new GetCategoryUseCase(categoryRepository);
        listCategories = new ListCategoriesUseCase(categoryRepository);
        activateCategory = new ActivateCategoryUseCase(categoryRepository, clock);
        deactivateCategory = new DeactivateCategoryUseCase(categoryRepository, clock);
    }

    @Test
    void shouldCreateCategoryAsActive() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResult result = createCategory.execute(new CreateCategoryCommand("Frutas", "Fresh produce"));

        assertEquals("Frutas", result.name());
        assertEquals("Fresh produce", result.description());
        assertEquals(CategoryStatus.ACTIVE, result.status());
        assertEquals(NOW, result.createdAt());
        assertEquals(NOW, result.updatedAt());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void shouldRejectPublicGetWhenCategoryIsInactive() {
        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(category(CATEGORY_ID, "Frutas", CategoryStatus.INACTIVE)));

        assertThrows(
                CategoryNotFoundException.class,
                () -> getCategory.execute(new GetCategoryCommand(CATEGORY_ID, CatalogView.PUBLIC)));
    }

    @Test
    void shouldRejectGetWhenCategoryIsMissing() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(
                CategoryNotFoundException.class,
                () -> getCategory.execute(new GetCategoryCommand(CATEGORY_ID, CatalogView.ADMIN)));
    }

    @Test
    void shouldRejectUpdateWhenCategoryIsMissing() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(
                CategoryNotFoundException.class,
                () -> updateCategory.execute(new UpdateCategoryCommand(CATEGORY_ID, "Verduras", null)));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void shouldRejectActivateWhenCategoryIsMissing() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(
                CategoryNotFoundException.class,
                () -> activateCategory.execute(new ActivateCategoryCommand(CATEGORY_ID)));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void shouldRejectDeactivateWhenCategoryIsMissing() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(
                CategoryNotFoundException.class,
                () -> deactivateCategory.execute(new DeactivateCategoryCommand(CATEGORY_ID)));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void shouldUpdateCategoryInformationWithoutChangingStatus() {
        Category existing = category(CATEGORY_ID, "Frutas", CategoryStatus.INACTIVE);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResult result =
                updateCategory.execute(new UpdateCategoryCommand(CATEGORY_ID, "Verduras", "Leafy greens"));

        assertEquals(CATEGORY_ID, result.id());
        assertEquals("Verduras", result.name());
        assertEquals("Leafy greens", result.description());
        assertEquals(CategoryStatus.INACTIVE, result.status());
        assertEquals(existing.createdAt(), result.createdAt());
        assertEquals(NOW, result.updatedAt());
    }

    @Test
    void shouldActivateCategory() {
        Category existing = category(CATEGORY_ID, "Frutas", CategoryStatus.INACTIVE);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResult result = activateCategory.execute(new ActivateCategoryCommand(CATEGORY_ID));

        assertEquals(CategoryStatus.ACTIVE, result.status());
        assertEquals(NOW, result.updatedAt());
    }

    @Test
    void shouldDeactivateCategory() {
        Category existing = category(CATEGORY_ID, "Frutas", CategoryStatus.ACTIVE);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResult result = deactivateCategory.execute(new DeactivateCategoryCommand(CATEGORY_ID));

        assertEquals(CategoryStatus.INACTIVE, result.status());
        assertEquals(NOW, result.updatedAt());
    }

    @Test
    void shouldListAllCategoriesForAdmin() {
        when(categoryRepository.findAll())
                .thenReturn(List.of(
                        category(CATEGORY_ID, "Frutas", CategoryStatus.ACTIVE),
                        category(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), "Abarrotes", CategoryStatus.INACTIVE)));

        List<CategoryResult> result = listCategories.execute(new ListCategoriesCommand(CatalogView.ADMIN));

        assertEquals(2, result.size());
        verify(categoryRepository).findAll();
    }

    @Test
    void shouldListOnlyActiveCategoriesForPublic() {
        Category active = category(CATEGORY_ID, "Frutas", CategoryStatus.ACTIVE);
        when(categoryRepository.findByStatus(CategoryStatus.ACTIVE)).thenReturn(List.of(active));

        List<CategoryResult> result = listCategories.execute(new ListCategoriesCommand(CatalogView.PUBLIC));

        assertEquals(1, result.size());
        assertEquals(CategoryStatus.ACTIVE, result.get(0).status());
        verify(categoryRepository).findByStatus(CategoryStatus.ACTIVE);
    }

    private static Category category(UUID id, String name, CategoryStatus status) {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        return Category.create(id, name, "desc", status, createdAt, createdAt);
    }
}

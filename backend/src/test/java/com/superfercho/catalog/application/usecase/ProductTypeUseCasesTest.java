package com.superfercho.catalog.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.dto.ActivateProductTypeCommand;
import com.superfercho.catalog.application.dto.CreateProductTypeCommand;
import com.superfercho.catalog.application.dto.DeactivateProductTypeCommand;
import com.superfercho.catalog.application.dto.GetProductTypeCommand;
import com.superfercho.catalog.application.dto.ListProductTypesCommand;
import com.superfercho.catalog.application.dto.ProductTypeResult;
import com.superfercho.catalog.application.dto.UpdateProductTypeCommand;
import com.superfercho.catalog.application.exception.InvalidCategoryReferenceException;
import com.superfercho.catalog.application.exception.ProductTypeNotFoundException;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.domain.exception.InvalidProductTypeException;
import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.CategoryStatus;
import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.domain.model.ProductTypeStatus;
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
class ProductTypeUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID CATEGORY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TYPE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock
    private ProductTypeRepository productTypeRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private CreateProductTypeUseCase createProductType;
    private GetProductTypeUseCase getProductType;
    private ListProductTypesUseCase listProductTypes;
    private UpdateProductTypeUseCase updateProductType;
    private ActivateProductTypeUseCase activateProductType;
    private DeactivateProductTypeUseCase deactivateProductType;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        createProductType = new CreateProductTypeUseCase(productTypeRepository, categoryRepository, clock);
        getProductType = new GetProductTypeUseCase(productTypeRepository);
        listProductTypes = new ListProductTypesUseCase(productTypeRepository);
        updateProductType = new UpdateProductTypeUseCase(productTypeRepository, clock);
        activateProductType = new ActivateProductTypeUseCase(productTypeRepository, clock);
        deactivateProductType = new DeactivateProductTypeUseCase(productTypeRepository, clock);
    }

    @Test
    void shouldCreateProductTypeAsActive() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category()));
        when(productTypeRepository.save(any(ProductType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductTypeResult result =
                createProductType.execute(new CreateProductTypeCommand(CATEGORY_ID, "Leche", "Lácteos líquidos"));

        assertEquals(CATEGORY_ID, result.categoryId());
        assertEquals("Leche", result.name());
        assertEquals("Lácteos líquidos", result.description());
        assertEquals(ProductTypeStatus.ACTIVE, result.status());
        assertEquals(NOW, result.createdAt());
        verify(productTypeRepository).save(any(ProductType.class));
    }

    @Test
    void shouldRejectCreateWhenCategoryIsMissing() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(
                InvalidCategoryReferenceException.class,
                () -> createProductType.execute(new CreateProductTypeCommand(CATEGORY_ID, "Leche", null)));
        verify(productTypeRepository, never()).save(any());
    }

    @Test
    void shouldRejectCreateWhenNameIsBlank() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category()));

        assertThrows(
                InvalidProductTypeException.class,
                () -> createProductType.execute(new CreateProductTypeCommand(CATEGORY_ID, "  ", null)));
        verify(productTypeRepository, never()).save(any());
    }

    @Test
    void shouldListProductTypesByCategory() {
        when(productTypeRepository.findByCategoryId(CATEGORY_ID))
                .thenReturn(List.of(productType(TYPE_ID, ProductTypeStatus.ACTIVE)));

        List<ProductTypeResult> results = listProductTypes.execute(new ListProductTypesCommand(CATEGORY_ID));

        assertEquals(1, results.size());
        assertEquals(TYPE_ID, results.getFirst().id());
        assertEquals(CATEGORY_ID, results.getFirst().categoryId());
    }

    @Test
    void shouldGetProductType() {
        when(productTypeRepository.findById(TYPE_ID))
                .thenReturn(Optional.of(productType(TYPE_ID, ProductTypeStatus.ACTIVE)));

        ProductTypeResult result = getProductType.execute(new GetProductTypeCommand(TYPE_ID));

        assertEquals(TYPE_ID, result.id());
        assertEquals("Leche", result.name());
    }

    @Test
    void shouldRejectGetWhenProductTypeIsMissing() {
        when(productTypeRepository.findById(TYPE_ID)).thenReturn(Optional.empty());

        assertThrows(
                ProductTypeNotFoundException.class,
                () -> getProductType.execute(new GetProductTypeCommand(TYPE_ID)));
    }

    @Test
    void shouldUpdateProductTypeWithoutChangingCategory() {
        when(productTypeRepository.findById(TYPE_ID))
                .thenReturn(Optional.of(productType(TYPE_ID, ProductTypeStatus.ACTIVE)));
        when(productTypeRepository.save(any(ProductType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductTypeResult result =
                updateProductType.execute(new UpdateProductTypeCommand(TYPE_ID, "Leche entera", "Actualizado"));

        assertEquals(CATEGORY_ID, result.categoryId());
        assertEquals("Leche entera", result.name());
        assertEquals("Actualizado", result.description());
        assertEquals(ProductTypeStatus.ACTIVE, result.status());
        assertEquals(CREATED_AT, result.createdAt());
        assertEquals(NOW, result.updatedAt());
    }

    @Test
    void shouldRejectUpdateWhenProductTypeIsMissing() {
        when(productTypeRepository.findById(TYPE_ID)).thenReturn(Optional.empty());

        assertThrows(
                ProductTypeNotFoundException.class,
                () -> updateProductType.execute(new UpdateProductTypeCommand(TYPE_ID, "Leche", null)));
        verify(productTypeRepository, never()).save(any());
    }

    @Test
    void shouldActivateProductType() {
        when(productTypeRepository.findById(TYPE_ID))
                .thenReturn(Optional.of(productType(TYPE_ID, ProductTypeStatus.INACTIVE)));
        when(productTypeRepository.save(any(ProductType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductTypeResult result = activateProductType.execute(new ActivateProductTypeCommand(TYPE_ID));

        assertEquals(ProductTypeStatus.ACTIVE, result.status());
        assertEquals(NOW, result.updatedAt());
    }

    @Test
    void shouldDeactivateProductType() {
        when(productTypeRepository.findById(TYPE_ID))
                .thenReturn(Optional.of(productType(TYPE_ID, ProductTypeStatus.ACTIVE)));
        when(productTypeRepository.save(any(ProductType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductTypeResult result = deactivateProductType.execute(new DeactivateProductTypeCommand(TYPE_ID));

        assertEquals(ProductTypeStatus.INACTIVE, result.status());
        assertEquals(NOW, result.updatedAt());
    }

    @Test
    void shouldRejectActivateWhenProductTypeIsMissing() {
        when(productTypeRepository.findById(TYPE_ID)).thenReturn(Optional.empty());

        assertThrows(
                ProductTypeNotFoundException.class,
                () -> activateProductType.execute(new ActivateProductTypeCommand(TYPE_ID)));
        verify(productTypeRepository, never()).save(any());
    }

    @Test
    void shouldRejectDeactivateWhenProductTypeIsMissing() {
        when(productTypeRepository.findById(TYPE_ID)).thenReturn(Optional.empty());

        assertThrows(
                ProductTypeNotFoundException.class,
                () -> deactivateProductType.execute(new DeactivateProductTypeCommand(TYPE_ID)));
        verify(productTypeRepository, never()).save(any());
    }

    private static Category category() {
        return Category.create(CATEGORY_ID, "Lácteos", null, CategoryStatus.ACTIVE, CREATED_AT, CREATED_AT);
    }

    private static ProductType productType(UUID id, ProductTypeStatus status) {
        return ProductType.create(id, CATEGORY_ID, "Leche", "Lácteos líquidos", status, CREATED_AT, CREATED_AT);
    }
}

package com.superfercho.catalog.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.dto.ActivateProductVariantCommand;
import com.superfercho.catalog.application.dto.CreateProductVariantCommand;
import com.superfercho.catalog.application.dto.DeactivateProductVariantCommand;
import com.superfercho.catalog.application.dto.GetProductVariantCommand;
import com.superfercho.catalog.application.dto.ListProductVariantsCommand;
import com.superfercho.catalog.application.dto.ProductVariantResult;
import com.superfercho.catalog.application.dto.UpdateProductVariantCommand;
import com.superfercho.catalog.application.exception.InvalidProductTypeReferenceException;
import com.superfercho.catalog.application.exception.ProductVariantNotFoundException;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.domain.exception.InvalidProductVariantException;
import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.domain.model.ProductTypeStatus;
import com.superfercho.catalog.domain.model.ProductVariant;
import com.superfercho.catalog.domain.model.ProductVariantStatus;
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
class ProductVariantUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID CATEGORY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TYPE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID VARIANT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ProductTypeRepository productTypeRepository;

    private CreateProductVariantUseCase createProductVariant;
    private GetProductVariantUseCase getProductVariant;
    private ListProductVariantsUseCase listProductVariants;
    private UpdateProductVariantUseCase updateProductVariant;
    private ActivateProductVariantUseCase activateProductVariant;
    private DeactivateProductVariantUseCase deactivateProductVariant;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        createProductVariant =
                new CreateProductVariantUseCase(productVariantRepository, productTypeRepository, clock);
        getProductVariant = new GetProductVariantUseCase(productVariantRepository);
        listProductVariants = new ListProductVariantsUseCase(productVariantRepository);
        updateProductVariant = new UpdateProductVariantUseCase(productVariantRepository, clock);
        activateProductVariant = new ActivateProductVariantUseCase(productVariantRepository, clock);
        deactivateProductVariant = new DeactivateProductVariantUseCase(productVariantRepository, clock);
    }

    @Test
    void shouldCreateProductVariantAsActive() {
        when(productTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(productType()));
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductVariantResult result = createProductVariant.execute(
                new CreateProductVariantCommand(TYPE_ID, "Deslactosada", "Sin lactosa"));

        assertEquals(TYPE_ID, result.productTypeId());
        assertEquals("Deslactosada", result.name());
        assertEquals(ProductVariantStatus.ACTIVE, result.status());
        assertEquals(NOW, result.createdAt());
        verify(productVariantRepository).save(any(ProductVariant.class));
    }

    @Test
    void shouldRejectCreateWhenProductTypeIsMissing() {
        when(productTypeRepository.findById(TYPE_ID)).thenReturn(Optional.empty());

        assertThrows(
                InvalidProductTypeReferenceException.class,
                () -> createProductVariant.execute(new CreateProductVariantCommand(TYPE_ID, "Entera", null)));
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    void shouldRejectCreateWhenNameIsBlank() {
        when(productTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(productType()));

        assertThrows(
                InvalidProductVariantException.class,
                () -> createProductVariant.execute(new CreateProductVariantCommand(TYPE_ID, " ", null)));
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    void shouldRejectCreateWhenNameExceedsMaxLength() {
        when(productTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(productType()));

        assertThrows(
                InvalidProductVariantException.class,
                () -> createProductVariant.execute(new CreateProductVariantCommand(
                        TYPE_ID, "n".repeat(ProductVariant.MAX_NAME_LENGTH + 1), null)));
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    void shouldListProductVariantsByType() {
        when(productVariantRepository.findByProductTypeId(TYPE_ID))
                .thenReturn(List.of(productVariant(VARIANT_ID, ProductVariantStatus.ACTIVE)));

        List<ProductVariantResult> results =
                listProductVariants.execute(new ListProductVariantsCommand(TYPE_ID));

        assertEquals(1, results.size());
        assertEquals(VARIANT_ID, results.getFirst().id());
        assertEquals(TYPE_ID, results.getFirst().productTypeId());
    }

    @Test
    void shouldGetProductVariant() {
        when(productVariantRepository.findById(VARIANT_ID))
                .thenReturn(Optional.of(productVariant(VARIANT_ID, ProductVariantStatus.ACTIVE)));

        ProductVariantResult result = getProductVariant.execute(new GetProductVariantCommand(VARIANT_ID));

        assertEquals(VARIANT_ID, result.id());
        assertEquals("Deslactosada", result.name());
    }

    @Test
    void shouldRejectGetWhenProductVariantIsMissing() {
        when(productVariantRepository.findById(VARIANT_ID)).thenReturn(Optional.empty());

        assertThrows(
                ProductVariantNotFoundException.class,
                () -> getProductVariant.execute(new GetProductVariantCommand(VARIANT_ID)));
    }

    @Test
    void shouldUpdateProductVariantWithoutChangingType() {
        when(productVariantRepository.findById(VARIANT_ID))
                .thenReturn(Optional.of(productVariant(VARIANT_ID, ProductVariantStatus.ACTIVE)));
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductVariantResult result = updateProductVariant.execute(
                new UpdateProductVariantCommand(VARIANT_ID, "Light", "Baja en grasa"));

        assertEquals(TYPE_ID, result.productTypeId());
        assertEquals("Light", result.name());
        assertEquals("Baja en grasa", result.description());
        assertEquals(CREATED_AT, result.createdAt());
        assertEquals(NOW, result.updatedAt());
    }

    @Test
    void shouldRejectUpdateWhenProductVariantIsMissing() {
        when(productVariantRepository.findById(VARIANT_ID)).thenReturn(Optional.empty());

        assertThrows(
                ProductVariantNotFoundException.class,
                () -> updateProductVariant.execute(new UpdateProductVariantCommand(VARIANT_ID, "Light", null)));
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    void shouldActivateProductVariant() {
        when(productVariantRepository.findById(VARIANT_ID))
                .thenReturn(Optional.of(productVariant(VARIANT_ID, ProductVariantStatus.INACTIVE)));
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductVariantResult result = activateProductVariant.execute(new ActivateProductVariantCommand(VARIANT_ID));

        assertEquals(ProductVariantStatus.ACTIVE, result.status());
    }

    @Test
    void shouldDeactivateProductVariant() {
        when(productVariantRepository.findById(VARIANT_ID))
                .thenReturn(Optional.of(productVariant(VARIANT_ID, ProductVariantStatus.ACTIVE)));
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductVariantResult result =
                deactivateProductVariant.execute(new DeactivateProductVariantCommand(VARIANT_ID));

        assertEquals(ProductVariantStatus.INACTIVE, result.status());
    }

    @Test
    void shouldRejectActivateWhenProductVariantIsMissing() {
        when(productVariantRepository.findById(VARIANT_ID)).thenReturn(Optional.empty());

        assertThrows(
                ProductVariantNotFoundException.class,
                () -> activateProductVariant.execute(new ActivateProductVariantCommand(VARIANT_ID)));
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    void shouldRejectDeactivateWhenProductVariantIsMissing() {
        when(productVariantRepository.findById(VARIANT_ID)).thenReturn(Optional.empty());

        assertThrows(
                ProductVariantNotFoundException.class,
                () -> deactivateProductVariant.execute(new DeactivateProductVariantCommand(VARIANT_ID)));
        verify(productVariantRepository, never()).save(any());
    }

    private static ProductType productType() {
        return ProductType.create(
                TYPE_ID, CATEGORY_ID, "Leche", null, ProductTypeStatus.ACTIVE, CREATED_AT, CREATED_AT);
    }

    private static ProductVariant productVariant(UUID id, ProductVariantStatus status) {
        return ProductVariant.create(id, TYPE_ID, "Deslactosada", "Sin lactosa", status, CREATED_AT, CREATED_AT);
    }
}

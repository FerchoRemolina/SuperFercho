package com.superfercho.catalog.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.catalog.application.dto.ActivateProductVariantCommand;
import com.superfercho.catalog.application.dto.CreateProductVariantCommand;
import com.superfercho.catalog.application.dto.DeactivateProductVariantCommand;
import com.superfercho.catalog.application.dto.GetProductVariantCommand;
import com.superfercho.catalog.application.dto.ListProductVariantsCommand;
import com.superfercho.catalog.application.dto.ProductVariantResult;
import com.superfercho.catalog.application.dto.UpdateProductVariantCommand;
import com.superfercho.catalog.application.exception.DuplicateProductVariantException;
import com.superfercho.catalog.application.exception.InvalidProductTypeReferenceException;
import com.superfercho.catalog.application.exception.ProductVariantNotFoundException;
import com.superfercho.catalog.application.usecase.ActivateProductVariantUseCase;
import com.superfercho.catalog.application.usecase.CreateProductVariantUseCase;
import com.superfercho.catalog.application.usecase.DeactivateProductVariantUseCase;
import com.superfercho.catalog.application.usecase.GetProductVariantUseCase;
import com.superfercho.catalog.application.usecase.ListProductVariantsUseCase;
import com.superfercho.catalog.application.usecase.UpdateProductVariantUseCase;
import com.superfercho.catalog.domain.model.ProductVariantStatus;
import com.superfercho.platform.error.ApiExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProductVariantController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CatalogExceptionHandler.class, ApiExceptionHandler.class})
class ProductVariantControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-03-01T10:30:00Z");
    private static final UUID TYPE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID VARIANT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateProductVariantUseCase createProductVariantUseCase;

    @MockitoBean
    private GetProductVariantUseCase getProductVariantUseCase;

    @MockitoBean
    private ListProductVariantsUseCase listProductVariantsUseCase;

    @MockitoBean
    private UpdateProductVariantUseCase updateProductVariantUseCase;

    @MockitoBean
    private ActivateProductVariantUseCase activateProductVariantUseCase;

    @MockitoBean
    private DeactivateProductVariantUseCase deactivateProductVariantUseCase;

    @Test
    void shouldCreateProductVariant() throws Exception {
        when(createProductVariantUseCase.execute(any())).thenReturn(variantResult(ProductVariantStatus.ACTIVE));

        mockMvc.perform(post("/api/v1/product-variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productTypeId": "%s",
                                  "name": "Deslactosada",
                                  "description": "Sin lactosa"
                                }
                                """.formatted(TYPE_ID)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", org.hamcrest.Matchers.endsWith("/api/v1/product-variants/" + VARIANT_ID)))
                .andExpect(jsonPath("$.id").value(VARIANT_ID.toString()))
                .andExpect(jsonPath("$.productTypeId").value(TYPE_ID.toString()))
                .andExpect(jsonPath("$.name").value("Deslactosada"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(createProductVariantUseCase)
                .execute(new CreateProductVariantCommand(TYPE_ID, "Deslactosada", "Sin lactosa"));
        verifyNoInteractions(
                getProductVariantUseCase,
                listProductVariantsUseCase,
                updateProductVariantUseCase,
                activateProductVariantUseCase,
                deactivateProductVariantUseCase);
    }

    @Test
    void shouldListProductVariantsByType() throws Exception {
        when(listProductVariantsUseCase.execute(new ListProductVariantsCommand(TYPE_ID)))
                .thenReturn(List.of(variantResult(ProductVariantStatus.ACTIVE)));

        mockMvc.perform(get("/api/v1/product-variants").param("productTypeId", TYPE_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(VARIANT_ID.toString()));

        verify(listProductVariantsUseCase).execute(new ListProductVariantsCommand(TYPE_ID));
    }

    @Test
    void shouldGetProductVariant() throws Exception {
        when(getProductVariantUseCase.execute(new GetProductVariantCommand(VARIANT_ID)))
                .thenReturn(variantResult(ProductVariantStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/product-variants/{productVariantId}", VARIANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VARIANT_ID.toString()));
    }

    @Test
    void shouldUpdateProductVariant() throws Exception {
        when(updateProductVariantUseCase.execute(any())).thenReturn(variantResult(ProductVariantStatus.ACTIVE));

        mockMvc.perform(put("/api/v1/product-variants/{productVariantId}", VARIANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Light", "description": "Baja en grasa"}
                                """))
                .andExpect(status().isOk());

        verify(updateProductVariantUseCase)
                .execute(new UpdateProductVariantCommand(VARIANT_ID, "Light", "Baja en grasa"));
    }

    @Test
    void shouldActivateProductVariant() throws Exception {
        when(activateProductVariantUseCase.execute(any())).thenReturn(variantResult(ProductVariantStatus.ACTIVE));

        mockMvc.perform(post("/api/v1/product-variants/{productVariantId}/activate", VARIANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(activateProductVariantUseCase).execute(new ActivateProductVariantCommand(VARIANT_ID));
    }

    @Test
    void shouldDeactivateProductVariant() throws Exception {
        when(deactivateProductVariantUseCase.execute(any()))
                .thenReturn(variantResult(ProductVariantStatus.INACTIVE));

        mockMvc.perform(post("/api/v1/product-variants/{productVariantId}/deactivate", VARIANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        verify(deactivateProductVariantUseCase).execute(new DeactivateProductVariantCommand(VARIANT_ID));
    }

    @Test
    void shouldMapDuplicateProductVariantTo409() throws Exception {
        when(createProductVariantUseCase.execute(any())).thenThrow(new DuplicateProductVariantException());

        mockMvc.perform(post("/api/v1/product-variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productTypeId": "%s", "name": "Entera", "description": null}
                                """.formatted(TYPE_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_PRODUCT_VARIANT"));
    }

    @Test
    void shouldMapInvalidProductTypeReferenceTo400() throws Exception {
        when(createProductVariantUseCase.execute(any()))
                .thenThrow(new InvalidProductTypeReferenceException(TYPE_ID));

        mockMvc.perform(post("/api/v1/product-variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productTypeId": "%s", "name": "Entera", "description": null}
                                """.formatted(TYPE_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PRODUCT_TYPE_REFERENCE"));
    }

    @Test
    void shouldMapProductVariantNotFoundTo404() throws Exception {
        when(getProductVariantUseCase.execute(any())).thenThrow(new ProductVariantNotFoundException(VARIANT_ID));

        mockMvc.perform(get("/api/v1/product-variants/{productVariantId}", VARIANT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_VARIANT_NOT_FOUND"));
    }

    private static ProductVariantResult variantResult(ProductVariantStatus status) {
        return new ProductVariantResult(
                VARIANT_ID, TYPE_ID, "Deslactosada", "Sin lactosa", status, CREATED_AT, UPDATED_AT);
    }
}

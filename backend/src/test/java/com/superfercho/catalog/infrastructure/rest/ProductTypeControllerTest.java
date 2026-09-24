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

import com.superfercho.catalog.application.dto.ActivateProductTypeCommand;
import com.superfercho.catalog.application.dto.CreateProductTypeCommand;
import com.superfercho.catalog.application.dto.DeactivateProductTypeCommand;
import com.superfercho.catalog.application.dto.GetProductTypeCommand;
import com.superfercho.catalog.application.dto.ListProductTypesCommand;
import com.superfercho.catalog.application.dto.ProductTypeResult;
import com.superfercho.catalog.application.dto.UpdateProductTypeCommand;
import com.superfercho.catalog.application.exception.DuplicateProductTypeException;
import com.superfercho.catalog.application.exception.InvalidCategoryReferenceException;
import com.superfercho.catalog.application.exception.ProductTypeNotFoundException;
import com.superfercho.catalog.application.usecase.ActivateProductTypeUseCase;
import com.superfercho.catalog.application.usecase.CreateProductTypeUseCase;
import com.superfercho.catalog.application.usecase.DeactivateProductTypeUseCase;
import com.superfercho.catalog.application.usecase.GetProductTypeUseCase;
import com.superfercho.catalog.application.usecase.ListProductTypesUseCase;
import com.superfercho.catalog.application.usecase.UpdateProductTypeUseCase;
import com.superfercho.catalog.domain.model.ProductTypeStatus;
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

@WebMvcTest(controllers = ProductTypeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CatalogExceptionHandler.class, ApiExceptionHandler.class})
class ProductTypeControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-03-01T10:30:00Z");
    private static final UUID CATEGORY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TYPE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateProductTypeUseCase createProductTypeUseCase;

    @MockitoBean
    private GetProductTypeUseCase getProductTypeUseCase;

    @MockitoBean
    private ListProductTypesUseCase listProductTypesUseCase;

    @MockitoBean
    private UpdateProductTypeUseCase updateProductTypeUseCase;

    @MockitoBean
    private ActivateProductTypeUseCase activateProductTypeUseCase;

    @MockitoBean
    private DeactivateProductTypeUseCase deactivateProductTypeUseCase;

    @Test
    void shouldCreateProductType() throws Exception {
        when(createProductTypeUseCase.execute(any())).thenReturn(typeResult(ProductTypeStatus.ACTIVE));

        mockMvc.perform(post("/api/v1/product-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": "%s",
                                  "name": "Leche",
                                  "description": "Lácteos líquidos"
                                }
                                """.formatted(CATEGORY_ID)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", org.hamcrest.Matchers.endsWith("/api/v1/product-types/" + TYPE_ID)))
                .andExpect(jsonPath("$.id").value(TYPE_ID.toString()))
                .andExpect(jsonPath("$.categoryId").value(CATEGORY_ID.toString()))
                .andExpect(jsonPath("$.name").value("Leche"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(createProductTypeUseCase)
                .execute(new CreateProductTypeCommand(CATEGORY_ID, "Leche", "Lácteos líquidos"));
        verifyNoInteractions(
                getProductTypeUseCase,
                listProductTypesUseCase,
                updateProductTypeUseCase,
                activateProductTypeUseCase,
                deactivateProductTypeUseCase);
    }

    @Test
    void shouldListProductTypesByCategory() throws Exception {
        when(listProductTypesUseCase.execute(new ListProductTypesCommand(CATEGORY_ID)))
                .thenReturn(List.of(typeResult(ProductTypeStatus.ACTIVE)));

        mockMvc.perform(get("/api/v1/product-types").param("categoryId", CATEGORY_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(TYPE_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("Leche"));

        verify(listProductTypesUseCase).execute(new ListProductTypesCommand(CATEGORY_ID));
    }

    @Test
    void shouldGetProductType() throws Exception {
        when(getProductTypeUseCase.execute(new GetProductTypeCommand(TYPE_ID)))
                .thenReturn(typeResult(ProductTypeStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/product-types/{productTypeId}", TYPE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TYPE_ID.toString()));

        verify(getProductTypeUseCase).execute(new GetProductTypeCommand(TYPE_ID));
    }

    @Test
    void shouldUpdateProductType() throws Exception {
        when(updateProductTypeUseCase.execute(any())).thenReturn(typeResult(ProductTypeStatus.ACTIVE));

        mockMvc.perform(put("/api/v1/product-types/{productTypeId}", TYPE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Leche entera", "description": "Actualizado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Leche"));

        verify(updateProductTypeUseCase)
                .execute(new UpdateProductTypeCommand(TYPE_ID, "Leche entera", "Actualizado"));
    }

    @Test
    void shouldActivateProductType() throws Exception {
        when(activateProductTypeUseCase.execute(any())).thenReturn(typeResult(ProductTypeStatus.ACTIVE));

        mockMvc.perform(post("/api/v1/product-types/{productTypeId}/activate", TYPE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(activateProductTypeUseCase).execute(new ActivateProductTypeCommand(TYPE_ID));
    }

    @Test
    void shouldDeactivateProductType() throws Exception {
        when(deactivateProductTypeUseCase.execute(any())).thenReturn(typeResult(ProductTypeStatus.INACTIVE));

        mockMvc.perform(post("/api/v1/product-types/{productTypeId}/deactivate", TYPE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        verify(deactivateProductTypeUseCase).execute(new DeactivateProductTypeCommand(TYPE_ID));
    }

    @Test
    void shouldMapDuplicateProductTypeTo409() throws Exception {
        when(createProductTypeUseCase.execute(any())).thenThrow(new DuplicateProductTypeException());

        mockMvc.perform(post("/api/v1/product-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId": "%s", "name": "Leche", "description": null}
                                """.formatted(CATEGORY_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_PRODUCT_TYPE"));
    }

    @Test
    void shouldMapInvalidCategoryReferenceTo400() throws Exception {
        when(createProductTypeUseCase.execute(any()))
                .thenThrow(new InvalidCategoryReferenceException(CATEGORY_ID));

        mockMvc.perform(post("/api/v1/product-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId": "%s", "name": "Leche", "description": null}
                                """.formatted(CATEGORY_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CATEGORY_REFERENCE"));
    }

    @Test
    void shouldMapProductTypeNotFoundTo404() throws Exception {
        when(getProductTypeUseCase.execute(any())).thenThrow(new ProductTypeNotFoundException(TYPE_ID));

        mockMvc.perform(get("/api/v1/product-types/{productTypeId}", TYPE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_TYPE_NOT_FOUND"));
    }

    private static ProductTypeResult typeResult(ProductTypeStatus status) {
        return new ProductTypeResult(
                TYPE_ID, CATEGORY_ID, "Leche", "Lácteos líquidos", status, CREATED_AT, UPDATED_AT);
    }
}

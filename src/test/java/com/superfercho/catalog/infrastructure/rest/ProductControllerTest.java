package com.superfercho.catalog.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.catalog.application.dto.ActivateProductCommand;
import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.ChangeProductPriceCommand;
import com.superfercho.catalog.application.dto.CreateProductCommand;
import com.superfercho.catalog.application.dto.DeactivateProductCommand;
import com.superfercho.catalog.application.dto.GetProductCommand;
import com.superfercho.catalog.application.dto.ListProductsCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.dto.SearchProductsCommand;
import com.superfercho.catalog.application.dto.UpdateProductCommand;
import com.superfercho.catalog.application.exception.DuplicateBarcodeException;
import com.superfercho.catalog.application.exception.InvalidCategoryReferenceException;
import com.superfercho.catalog.application.exception.ProductNotFoundException;
import com.superfercho.catalog.application.usecase.ActivateProductUseCase;
import com.superfercho.catalog.application.usecase.ChangeProductPriceUseCase;
import com.superfercho.catalog.application.usecase.CreateProductUseCase;
import com.superfercho.catalog.application.usecase.DeactivateProductUseCase;
import com.superfercho.catalog.application.usecase.GetProductUseCase;
import com.superfercho.catalog.application.usecase.ListProductsUseCase;
import com.superfercho.catalog.application.usecase.SearchProductsUseCase;
import com.superfercho.catalog.application.usecase.UpdateProductUseCase;
import com.superfercho.catalog.domain.exception.InvalidProductException;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.platform.error.ApiExceptionHandler;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
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

@WebMvcTest(controllers = ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CatalogExceptionHandler.class, ApiExceptionHandler.class})
class ProductControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-03-01T10:30:00Z");
    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CATEGORY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Money PRICE = Money.cop(new BigDecimal("10.50"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateProductUseCase createProductUseCase;

    @MockitoBean
    private GetProductUseCase getProductUseCase;

    @MockitoBean
    private ListProductsUseCase listProductsUseCase;

    @MockitoBean
    private SearchProductsUseCase searchProductsUseCase;

    @MockitoBean
    private UpdateProductUseCase updateProductUseCase;

    @MockitoBean
    private ActivateProductUseCase activateProductUseCase;

    @MockitoBean
    private DeactivateProductUseCase deactivateProductUseCase;

    @MockitoBean
    private ChangeProductPriceUseCase changeProductPriceUseCase;

    @Test
    void shouldCreateProduct() throws Exception {
        when(createProductUseCase.execute(any())).thenReturn(productResult(ProductStatus.ACTIVE, PRICE));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createProductJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", org.hamcrest.Matchers.endsWith("/api/v1/products/" + PRODUCT_ID)))
                .andExpect(jsonPath("$.id").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.categoryId").value(CATEGORY_ID.toString()))
                .andExpect(jsonPath("$.barcode").value("7701234567890"))
                .andExpect(jsonPath("$.name").value("Leche entera"))
                .andExpect(jsonPath("$.brand").value("Alquería"))
                .andExpect(jsonPath("$.description").value("Bolsa 1L"))
                .andExpect(jsonPath("$.price.amount").value(10.50))
                .andExpect(jsonPath("$.price.currency").value("COP"))
                .andExpect(jsonPath("$.stock").value(20))
                .andExpect(jsonPath("$.imageUrl").value("https://img.test/leche.png"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.updatedAt").value(UPDATED_AT.toString()));

        verify(createProductUseCase)
                .execute(new CreateProductCommand(
                        CATEGORY_ID,
                        "7701234567890",
                        "Leche entera",
                        "Alquería",
                        "Bolsa 1L",
                        PRICE,
                        20,
                        "https://img.test/leche.png"));
        verifyNoInteractions(
                getProductUseCase,
                listProductsUseCase,
                searchProductsUseCase,
                updateProductUseCase,
                activateProductUseCase,
                deactivateProductUseCase,
                changeProductPriceUseCase);
    }

    @Test
    void shouldListProductsWithPublicViewByDefault() throws Exception {
        when(listProductsUseCase.execute(new ListProductsCommand(null, null, CatalogView.PUBLIC)))
                .thenReturn(List.of(productResult(ProductStatus.ACTIVE, PRICE)));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("Leche entera"));

        verify(listProductsUseCase).execute(new ListProductsCommand(null, null, CatalogView.PUBLIC));
        verifyNoInteractions(
                createProductUseCase,
                getProductUseCase,
                searchProductsUseCase,
                updateProductUseCase,
                activateProductUseCase,
                deactivateProductUseCase,
                changeProductPriceUseCase);
    }

    @Test
    void shouldListProductsWithFiltersAndAdminView() throws Exception {
        when(listProductsUseCase.execute(
                        new ListProductsCommand(CATEGORY_ID, ProductStatus.INACTIVE, CatalogView.ADMIN)))
                .thenReturn(List.of(productResult(ProductStatus.INACTIVE, PRICE)));

        mockMvc.perform(get("/api/v1/products")
                        .param("categoryId", CATEGORY_ID.toString())
                        .param("status", "INACTIVE")
                        .param("view", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("INACTIVE"));

        verify(listProductsUseCase)
                .execute(new ListProductsCommand(CATEGORY_ID, ProductStatus.INACTIVE, CatalogView.ADMIN));
        verify(listProductsUseCase, never()).execute(new ListProductsCommand(null, null, CatalogView.PUBLIC));
    }

    @Test
    void shouldSearchProductsWithPublicViewByDefault() throws Exception {
        when(searchProductsUseCase.execute(new SearchProductsCommand("leche", CatalogView.PUBLIC)))
                .thenReturn(List.of(productResult(ProductStatus.ACTIVE, PRICE)));

        mockMvc.perform(get("/api/v1/products/search").param("text", "leche"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("Leche entera"));

        verify(searchProductsUseCase).execute(new SearchProductsCommand("leche", CatalogView.PUBLIC));
        verifyNoInteractions(getProductUseCase, listProductsUseCase);
    }

    @Test
    void shouldSearchProductsWithAdminView() throws Exception {
        when(searchProductsUseCase.execute(new SearchProductsCommand("leche", CatalogView.ADMIN)))
                .thenReturn(List.of(productResult(ProductStatus.INACTIVE, PRICE)));

        mockMvc.perform(get("/api/v1/products/search").param("text", "leche").param("view", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("INACTIVE"));

        verify(searchProductsUseCase).execute(new SearchProductsCommand("leche", CatalogView.ADMIN));
        verifyNoInteractions(getProductUseCase, listProductsUseCase);
    }

    @Test
    void shouldSearchProductsWithoutText() throws Exception {
        when(searchProductsUseCase.execute(new SearchProductsCommand(null, CatalogView.PUBLIC))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/products/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(searchProductsUseCase).execute(new SearchProductsCommand(null, CatalogView.PUBLIC));
        verifyNoInteractions(getProductUseCase);
    }

    @Test
    void shouldGetProductByIdWithPublicViewByDefault() throws Exception {
        when(getProductUseCase.execute(new GetProductCommand(PRODUCT_ID, CatalogView.PUBLIC)))
                .thenReturn(productResult(ProductStatus.ACTIVE, PRICE));

        mockMvc.perform(get("/api/v1/products/{productId}", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.barcode").value("7701234567890"))
                .andExpect(jsonPath("$.price.amount").value(10.50));

        verify(getProductUseCase).execute(new GetProductCommand(PRODUCT_ID, CatalogView.PUBLIC));
        verifyNoInteractions(searchProductsUseCase, listProductsUseCase);
    }

    @Test
    void shouldGetProductByIdWithAdminView() throws Exception {
        when(getProductUseCase.execute(new GetProductCommand(PRODUCT_ID, CatalogView.ADMIN)))
                .thenReturn(productResult(ProductStatus.INACTIVE, PRICE));

        mockMvc.perform(get("/api/v1/products/{productId}", PRODUCT_ID).param("view", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        verify(getProductUseCase).execute(new GetProductCommand(PRODUCT_ID, CatalogView.ADMIN));
    }

    @Test
    void shouldUpdateProduct() throws Exception {
        when(updateProductUseCase.execute(any())).thenReturn(productResult(ProductStatus.ACTIVE, PRICE));

        mockMvc.perform(put("/api/v1/products/{productId}", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateProductJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID.toString()));

        verify(updateProductUseCase)
                .execute(new UpdateProductCommand(
                        PRODUCT_ID,
                        CATEGORY_ID,
                        "7701234567890",
                        "Leche deslactosada",
                        "Alquería",
                        "Bolsa 900ml",
                        "https://img.test/leche-deslactosada.png"));
        verifyNoInteractions(createProductUseCase, changeProductPriceUseCase);
    }

    @Test
    void shouldActivateProduct() throws Exception {
        when(activateProductUseCase.execute(new ActivateProductCommand(PRODUCT_ID)))
                .thenReturn(productResult(ProductStatus.ACTIVE, PRICE));

        mockMvc.perform(post("/api/v1/products/{productId}/activate", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(activateProductUseCase).execute(new ActivateProductCommand(PRODUCT_ID));
        verifyNoInteractions(deactivateProductUseCase, changeProductPriceUseCase);
    }

    @Test
    void shouldDeactivateProduct() throws Exception {
        when(deactivateProductUseCase.execute(new DeactivateProductCommand(PRODUCT_ID)))
                .thenReturn(productResult(ProductStatus.INACTIVE, PRICE));

        mockMvc.perform(post("/api/v1/products/{productId}/deactivate", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        verify(deactivateProductUseCase).execute(new DeactivateProductCommand(PRODUCT_ID));
        verifyNoInteractions(activateProductUseCase);
    }

    @Test
    void shouldChangeProductPrice() throws Exception {
        Money newPrice = Money.cop(new BigDecimal("12.00"));
        when(changeProductPriceUseCase.execute(any())).thenReturn(productResult(ProductStatus.ACTIVE, newPrice));

        mockMvc.perform(post("/api/v1/products/{productId}/price", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price": {"amount": 12.00, "currency": "COP"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price.amount").value(12.00))
                .andExpect(jsonPath("$.price.currency").value("COP"));

        verify(changeProductPriceUseCase).execute(new ChangeProductPriceCommand(PRODUCT_ID, newPrice));
        verifyNoInteractions(updateProductUseCase);
    }

    @Test
    void shouldMapProductNotFoundToNotFound() throws Exception {
        when(getProductUseCase.execute(any())).thenThrow(new ProductNotFoundException(PRODUCT_ID));

        mockMvc.perform(get("/api/v1/products/{productId}", PRODUCT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Product not found: " + PRODUCT_ID))
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void shouldMapDuplicateBarcodeToConflict() throws Exception {
        when(createProductUseCase.execute(any())).thenThrow(new DuplicateBarcodeException());

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createProductJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.code").value("DUPLICATE_BARCODE"));
    }

    @Test
    void shouldMapInvalidCategoryReferenceToBadRequest() throws Exception {
        when(createProductUseCase.execute(any())).thenThrow(new InvalidCategoryReferenceException(CATEGORY_ID));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createProductJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CATEGORY_REFERENCE"));
    }

    @Test
    void shouldMapInvalidProductToBadRequest() throws Exception {
        when(createProductUseCase.execute(any())).thenThrow(new InvalidProductException("name cannot be null or blank"));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createProductJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PRODUCT"));
    }

    @Test
    void shouldRejectMalformedCreateBody() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createProductUseCase);
    }

    @Test
    void shouldRejectInvalidProductId() throws Exception {
        mockMvc.perform(get("/api/v1/products/{productId}", "not-a-uuid")).andExpect(status().isBadRequest());

        verifyNoInteractions(getProductUseCase, searchProductsUseCase);
    }

    private static String createProductJson() {
        return """
                {
                  "categoryId": "%s",
                  "barcode": "7701234567890",
                  "name": "Leche entera",
                  "brand": "Alquería",
                  "description": "Bolsa 1L",
                  "price": {"amount": 10.50, "currency": "COP"},
                  "stock": 20,
                  "imageUrl": "https://img.test/leche.png"
                }
                """.formatted(CATEGORY_ID);
    }

    private static String updateProductJson() {
        return """
                {
                  "categoryId": "%s",
                  "barcode": "7701234567890",
                  "name": "Leche deslactosada",
                  "brand": "Alquería",
                  "description": "Bolsa 900ml",
                  "imageUrl": "https://img.test/leche-deslactosada.png"
                }
                """.formatted(CATEGORY_ID);
    }

    private static ProductResult productResult(ProductStatus status, Money price) {
        return new ProductResult(
                PRODUCT_ID,
                CATEGORY_ID,
                "7701234567890",
                "Leche entera",
                "Alquería",
                "Bolsa 1L",
                price,
                20,
                "https://img.test/leche.png",
                status,
                CREATED_AT,
                UPDATED_AT);
    }
}

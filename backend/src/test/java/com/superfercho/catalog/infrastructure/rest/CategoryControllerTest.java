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

import com.superfercho.catalog.application.dto.ActivateCategoryCommand;
import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.CategoryResult;
import com.superfercho.catalog.application.dto.CreateCategoryCommand;
import com.superfercho.catalog.application.dto.DeactivateCategoryCommand;
import com.superfercho.catalog.application.dto.GetCategoryCommand;
import com.superfercho.catalog.application.dto.ListCategoriesCommand;
import com.superfercho.catalog.application.dto.UpdateCategoryCommand;
import com.superfercho.catalog.application.exception.CategoryNotFoundException;
import com.superfercho.catalog.application.usecase.ActivateCategoryUseCase;
import com.superfercho.catalog.application.usecase.CreateCategoryUseCase;
import com.superfercho.catalog.application.usecase.DeactivateCategoryUseCase;
import com.superfercho.catalog.application.usecase.GetCategoryUseCase;
import com.superfercho.catalog.application.usecase.ListCategoriesUseCase;
import com.superfercho.catalog.application.usecase.UpdateCategoryUseCase;
import com.superfercho.catalog.domain.exception.InvalidCategoryException;
import com.superfercho.catalog.domain.model.CategoryStatus;
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

@WebMvcTest(controllers = CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CatalogExceptionHandler.class, ApiExceptionHandler.class})
class CategoryControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-03-01T10:30:00Z");
    private static final UUID CATEGORY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCategoryUseCase createCategoryUseCase;

    @MockitoBean
    private GetCategoryUseCase getCategoryUseCase;

    @MockitoBean
    private ListCategoriesUseCase listCategoriesUseCase;

    @MockitoBean
    private UpdateCategoryUseCase updateCategoryUseCase;

    @MockitoBean
    private ActivateCategoryUseCase activateCategoryUseCase;

    @MockitoBean
    private DeactivateCategoryUseCase deactivateCategoryUseCase;

    @Test
    void shouldCreateCategory() throws Exception {
        when(createCategoryUseCase.execute(any())).thenReturn(categoryResult(CategoryStatus.ACTIVE));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("Lácteos", "Leche y derivados")))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", org.hamcrest.Matchers.endsWith("/api/v1/categories/" + CATEGORY_ID)))
                .andExpect(jsonPath("$.id").value(CATEGORY_ID.toString()))
                .andExpect(jsonPath("$.name").value("Lácteos"))
                .andExpect(jsonPath("$.description").value("Leche y derivados"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.updatedAt").value(UPDATED_AT.toString()));

        verify(createCategoryUseCase).execute(new CreateCategoryCommand("Lácteos", "Leche y derivados"));
        verifyNoInteractions(
                getCategoryUseCase,
                listCategoriesUseCase,
                updateCategoryUseCase,
                activateCategoryUseCase,
                deactivateCategoryUseCase);
    }

    @Test
    void shouldListCategoriesWithPublicViewByDefault() throws Exception {
        when(listCategoriesUseCase.execute(new ListCategoriesCommand(CatalogView.PUBLIC)))
                .thenReturn(List.of(categoryResult(CategoryStatus.ACTIVE)));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(CATEGORY_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("Lácteos"));

        verify(listCategoriesUseCase).execute(new ListCategoriesCommand(CatalogView.PUBLIC));
        verify(listCategoriesUseCase, never()).execute(new ListCategoriesCommand(CatalogView.ADMIN));
        verifyNoInteractions(
                createCategoryUseCase,
                getCategoryUseCase,
                updateCategoryUseCase,
                activateCategoryUseCase,
                deactivateCategoryUseCase);
    }

    @Test
    void shouldListCategoriesWithAdminView() throws Exception {
        when(listCategoriesUseCase.execute(new ListCategoriesCommand(CatalogView.ADMIN)))
                .thenReturn(List.of(categoryResult(CategoryStatus.INACTIVE)));

        mockMvc.perform(get("/api/v1/categories").param("view", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("INACTIVE"));

        verify(listCategoriesUseCase).execute(new ListCategoriesCommand(CatalogView.ADMIN));
    }

    @Test
    void shouldGetCategoryByIdWithPublicViewByDefault() throws Exception {
        when(getCategoryUseCase.execute(new GetCategoryCommand(CATEGORY_ID, CatalogView.PUBLIC)))
                .thenReturn(categoryResult(CategoryStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/categories/{categoryId}", CATEGORY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CATEGORY_ID.toString()))
                .andExpect(jsonPath("$.name").value("Lácteos"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(getCategoryUseCase).execute(new GetCategoryCommand(CATEGORY_ID, CatalogView.PUBLIC));
        verifyNoInteractions(
                createCategoryUseCase,
                listCategoriesUseCase,
                updateCategoryUseCase,
                activateCategoryUseCase,
                deactivateCategoryUseCase);
    }

    @Test
    void shouldGetCategoryByIdWithAdminView() throws Exception {
        when(getCategoryUseCase.execute(new GetCategoryCommand(CATEGORY_ID, CatalogView.ADMIN)))
                .thenReturn(categoryResult(CategoryStatus.INACTIVE));

        mockMvc.perform(get("/api/v1/categories/{categoryId}", CATEGORY_ID).param("view", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        verify(getCategoryUseCase).execute(new GetCategoryCommand(CATEGORY_ID, CatalogView.ADMIN));
    }

    @Test
    void shouldUpdateCategory() throws Exception {
        when(updateCategoryUseCase.execute(any())).thenReturn(categoryResult(CategoryStatus.ACTIVE));

        mockMvc.perform(put("/api/v1/categories/{categoryId}", CATEGORY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("Lácteos frescos", "Actualizada")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CATEGORY_ID.toString()));

        verify(updateCategoryUseCase)
                .execute(new UpdateCategoryCommand(CATEGORY_ID, "Lácteos frescos", "Actualizada"));
        verifyNoInteractions(
                createCategoryUseCase,
                getCategoryUseCase,
                listCategoriesUseCase,
                activateCategoryUseCase,
                deactivateCategoryUseCase);
    }

    @Test
    void shouldActivateCategory() throws Exception {
        when(activateCategoryUseCase.execute(new ActivateCategoryCommand(CATEGORY_ID)))
                .thenReturn(categoryResult(CategoryStatus.ACTIVE));

        mockMvc.perform(post("/api/v1/categories/{categoryId}/activate", CATEGORY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CATEGORY_ID.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(activateCategoryUseCase).execute(new ActivateCategoryCommand(CATEGORY_ID));
        verifyNoInteractions(
                createCategoryUseCase,
                getCategoryUseCase,
                listCategoriesUseCase,
                updateCategoryUseCase,
                deactivateCategoryUseCase);
    }

    @Test
    void shouldDeactivateCategory() throws Exception {
        when(deactivateCategoryUseCase.execute(new DeactivateCategoryCommand(CATEGORY_ID)))
                .thenReturn(categoryResult(CategoryStatus.INACTIVE));

        mockMvc.perform(post("/api/v1/categories/{categoryId}/deactivate", CATEGORY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CATEGORY_ID.toString()))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        verify(deactivateCategoryUseCase).execute(new DeactivateCategoryCommand(CATEGORY_ID));
        verifyNoInteractions(
                createCategoryUseCase,
                getCategoryUseCase,
                listCategoriesUseCase,
                updateCategoryUseCase,
                activateCategoryUseCase);
    }

    @Test
    void shouldMapCategoryNotFoundToNotFound() throws Exception {
        when(getCategoryUseCase.execute(any())).thenThrow(new CategoryNotFoundException(CATEGORY_ID));

        mockMvc.perform(get("/api/v1/categories/{categoryId}", CATEGORY_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Category not found: " + CATEGORY_ID))
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void shouldMapInvalidCategoryToBadRequest() throws Exception {
        when(createCategoryUseCase.execute(any())).thenThrow(new InvalidCategoryException("name cannot be null or blank"));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("Lácteos", "Leche y derivados")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.code").value("INVALID_CATEGORY"));
    }

    @Test
    void shouldRejectMalformedCreateBody() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(
                createCategoryUseCase,
                getCategoryUseCase,
                listCategoriesUseCase,
                updateCategoryUseCase,
                activateCategoryUseCase,
                deactivateCategoryUseCase);
    }

    @Test
    void shouldRejectInvalidCategoryId() throws Exception {
        mockMvc.perform(get("/api/v1/categories/{categoryId}", "not-a-uuid")).andExpect(status().isBadRequest());

        verifyNoInteractions(getCategoryUseCase);
    }

    @Test
    void shouldRejectInvalidCatalogView() throws Exception {
        mockMvc.perform(get("/api/v1/categories").param("view", "UNKNOWN")).andExpect(status().isBadRequest());

        verifyNoInteractions(listCategoriesUseCase);
    }

    private static String categoryJson(String name, String description) {
        return """
                {
                  "name": "%s",
                  "description": "%s"
                }
                """.formatted(name, description);
    }

    private static CategoryResult categoryResult(CategoryStatus status) {
        return new CategoryResult(
                CATEGORY_ID, "Lácteos", "Leche y derivados", status, CREATED_AT, UPDATED_AT);
    }
}

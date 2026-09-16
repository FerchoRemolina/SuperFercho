package com.superfercho.catalog.infrastructure.rest;

import com.superfercho.catalog.application.dto.ActivateCategoryCommand;
import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.CreateCategoryCommand;
import com.superfercho.catalog.application.dto.DeactivateCategoryCommand;
import com.superfercho.catalog.application.dto.GetCategoryCommand;
import com.superfercho.catalog.application.dto.ListCategoriesCommand;
import com.superfercho.catalog.application.dto.UpdateCategoryCommand;
import com.superfercho.catalog.application.usecase.ActivateCategoryUseCase;
import com.superfercho.catalog.application.usecase.CreateCategoryUseCase;
import com.superfercho.catalog.application.usecase.DeactivateCategoryUseCase;
import com.superfercho.catalog.application.usecase.GetCategoryUseCase;
import com.superfercho.catalog.application.usecase.ListCategoriesUseCase;
import com.superfercho.catalog.application.usecase.UpdateCategoryUseCase;
import com.superfercho.catalog.infrastructure.rest.dto.CategoryRestResponse;
import com.superfercho.catalog.infrastructure.rest.dto.CreateCategoryRequest;
import com.superfercho.catalog.infrastructure.rest.dto.UpdateCategoryRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@Profile("!test")
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CreateCategoryUseCase createCategoryUseCase;
    private final GetCategoryUseCase getCategoryUseCase;
    private final ListCategoriesUseCase listCategoriesUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final ActivateCategoryUseCase activateCategoryUseCase;
    private final DeactivateCategoryUseCase deactivateCategoryUseCase;

    public CategoryController(
            CreateCategoryUseCase createCategoryUseCase,
            GetCategoryUseCase getCategoryUseCase,
            ListCategoriesUseCase listCategoriesUseCase,
            UpdateCategoryUseCase updateCategoryUseCase,
            ActivateCategoryUseCase activateCategoryUseCase,
            DeactivateCategoryUseCase deactivateCategoryUseCase) {
        this.createCategoryUseCase = createCategoryUseCase;
        this.getCategoryUseCase = getCategoryUseCase;
        this.listCategoriesUseCase = listCategoriesUseCase;
        this.updateCategoryUseCase = updateCategoryUseCase;
        this.activateCategoryUseCase = activateCategoryUseCase;
        this.deactivateCategoryUseCase = deactivateCategoryUseCase;
    }

    @PostMapping
    public ResponseEntity<CategoryRestResponse> create(@RequestBody CreateCategoryRequest request) {
        CategoryRestResponse body = CategoryRestResponse.from(
                createCategoryUseCase.execute(new CreateCategoryCommand(request.name(), request.description())));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{categoryId}")
                        .buildAndExpand(body.id())
                        .toUri())
                .body(body);
    }

    @GetMapping
    public List<CategoryRestResponse> list(@RequestParam(required = false) CatalogView view) {
        return listCategoriesUseCase.execute(new ListCategoriesCommand(catalogView(view))).stream()
                .map(CategoryRestResponse::from)
                .toList();
    }

    @GetMapping("/{categoryId}")
    public CategoryRestResponse get(
            @PathVariable UUID categoryId, @RequestParam(required = false) CatalogView view) {
        return CategoryRestResponse.from(
                getCategoryUseCase.execute(new GetCategoryCommand(categoryId, catalogView(view))));
    }

    @PutMapping("/{categoryId}")
    public CategoryRestResponse update(
            @PathVariable UUID categoryId, @RequestBody UpdateCategoryRequest request) {
        return CategoryRestResponse.from(updateCategoryUseCase.execute(
                new UpdateCategoryCommand(categoryId, request.name(), request.description())));
    }

    @PostMapping("/{categoryId}/activate")
    public CategoryRestResponse activate(@PathVariable UUID categoryId) {
        return CategoryRestResponse.from(
                activateCategoryUseCase.execute(new ActivateCategoryCommand(categoryId)));
    }

    @PostMapping("/{categoryId}/deactivate")
    public CategoryRestResponse deactivate(@PathVariable UUID categoryId) {
        return CategoryRestResponse.from(
                deactivateCategoryUseCase.execute(new DeactivateCategoryCommand(categoryId)));
    }

    private static CatalogView catalogView(CatalogView view) {
        return view == null ? CatalogView.PUBLIC : view;
    }
}

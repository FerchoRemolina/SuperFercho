package com.superfercho.catalog.infrastructure.rest;

import com.superfercho.catalog.application.dto.ActivateProductTypeCommand;
import com.superfercho.catalog.application.dto.CreateProductTypeCommand;
import com.superfercho.catalog.application.dto.DeactivateProductTypeCommand;
import com.superfercho.catalog.application.dto.GetProductTypeCommand;
import com.superfercho.catalog.application.dto.ListProductTypesCommand;
import com.superfercho.catalog.application.dto.UpdateProductTypeCommand;
import com.superfercho.catalog.application.usecase.ActivateProductTypeUseCase;
import com.superfercho.catalog.application.usecase.CreateProductTypeUseCase;
import com.superfercho.catalog.application.usecase.DeactivateProductTypeUseCase;
import com.superfercho.catalog.application.usecase.GetProductTypeUseCase;
import com.superfercho.catalog.application.usecase.ListProductTypesUseCase;
import com.superfercho.catalog.application.usecase.UpdateProductTypeUseCase;
import com.superfercho.catalog.infrastructure.rest.dto.CreateProductTypeRequest;
import com.superfercho.catalog.infrastructure.rest.dto.ProductTypeRestResponse;
import com.superfercho.catalog.infrastructure.rest.dto.UpdateProductTypeRequest;
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
@RequestMapping("/api/v1/product-types")
public class ProductTypeController {

    private final CreateProductTypeUseCase createProductTypeUseCase;
    private final GetProductTypeUseCase getProductTypeUseCase;
    private final ListProductTypesUseCase listProductTypesUseCase;
    private final UpdateProductTypeUseCase updateProductTypeUseCase;
    private final ActivateProductTypeUseCase activateProductTypeUseCase;
    private final DeactivateProductTypeUseCase deactivateProductTypeUseCase;

    public ProductTypeController(
            CreateProductTypeUseCase createProductTypeUseCase,
            GetProductTypeUseCase getProductTypeUseCase,
            ListProductTypesUseCase listProductTypesUseCase,
            UpdateProductTypeUseCase updateProductTypeUseCase,
            ActivateProductTypeUseCase activateProductTypeUseCase,
            DeactivateProductTypeUseCase deactivateProductTypeUseCase) {
        this.createProductTypeUseCase = createProductTypeUseCase;
        this.getProductTypeUseCase = getProductTypeUseCase;
        this.listProductTypesUseCase = listProductTypesUseCase;
        this.updateProductTypeUseCase = updateProductTypeUseCase;
        this.activateProductTypeUseCase = activateProductTypeUseCase;
        this.deactivateProductTypeUseCase = deactivateProductTypeUseCase;
    }

    @PostMapping
    public ResponseEntity<ProductTypeRestResponse> create(@RequestBody CreateProductTypeRequest request) {
        ProductTypeRestResponse body = ProductTypeRestResponse.from(createProductTypeUseCase.execute(
                new CreateProductTypeCommand(request.categoryId(), request.name(), request.description())));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{productTypeId}")
                        .buildAndExpand(body.id())
                        .toUri())
                .body(body);
    }

    @GetMapping
    public List<ProductTypeRestResponse> list(@RequestParam UUID categoryId) {
        return listProductTypesUseCase.execute(new ListProductTypesCommand(categoryId)).stream()
                .map(ProductTypeRestResponse::from)
                .toList();
    }

    @GetMapping("/{productTypeId}")
    public ProductTypeRestResponse get(@PathVariable UUID productTypeId) {
        return ProductTypeRestResponse.from(
                getProductTypeUseCase.execute(new GetProductTypeCommand(productTypeId)));
    }

    @PutMapping("/{productTypeId}")
    public ProductTypeRestResponse update(
            @PathVariable UUID productTypeId, @RequestBody UpdateProductTypeRequest request) {
        return ProductTypeRestResponse.from(updateProductTypeUseCase.execute(
                new UpdateProductTypeCommand(productTypeId, request.name(), request.description())));
    }

    @PostMapping("/{productTypeId}/activate")
    public ProductTypeRestResponse activate(@PathVariable UUID productTypeId) {
        return ProductTypeRestResponse.from(
                activateProductTypeUseCase.execute(new ActivateProductTypeCommand(productTypeId)));
    }

    @PostMapping("/{productTypeId}/deactivate")
    public ProductTypeRestResponse deactivate(@PathVariable UUID productTypeId) {
        return ProductTypeRestResponse.from(
                deactivateProductTypeUseCase.execute(new DeactivateProductTypeCommand(productTypeId)));
    }
}

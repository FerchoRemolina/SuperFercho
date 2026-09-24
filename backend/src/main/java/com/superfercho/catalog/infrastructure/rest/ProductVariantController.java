package com.superfercho.catalog.infrastructure.rest;

import com.superfercho.catalog.application.dto.ActivateProductVariantCommand;
import com.superfercho.catalog.application.dto.CreateProductVariantCommand;
import com.superfercho.catalog.application.dto.DeactivateProductVariantCommand;
import com.superfercho.catalog.application.dto.GetProductVariantCommand;
import com.superfercho.catalog.application.dto.ListProductVariantsCommand;
import com.superfercho.catalog.application.dto.UpdateProductVariantCommand;
import com.superfercho.catalog.application.usecase.ActivateProductVariantUseCase;
import com.superfercho.catalog.application.usecase.CreateProductVariantUseCase;
import com.superfercho.catalog.application.usecase.DeactivateProductVariantUseCase;
import com.superfercho.catalog.application.usecase.GetProductVariantUseCase;
import com.superfercho.catalog.application.usecase.ListProductVariantsUseCase;
import com.superfercho.catalog.application.usecase.UpdateProductVariantUseCase;
import com.superfercho.catalog.infrastructure.rest.dto.CreateProductVariantRequest;
import com.superfercho.catalog.infrastructure.rest.dto.ProductVariantRestResponse;
import com.superfercho.catalog.infrastructure.rest.dto.UpdateProductVariantRequest;
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
@RequestMapping("/api/v1/product-variants")
public class ProductVariantController {

    private final CreateProductVariantUseCase createProductVariantUseCase;
    private final GetProductVariantUseCase getProductVariantUseCase;
    private final ListProductVariantsUseCase listProductVariantsUseCase;
    private final UpdateProductVariantUseCase updateProductVariantUseCase;
    private final ActivateProductVariantUseCase activateProductVariantUseCase;
    private final DeactivateProductVariantUseCase deactivateProductVariantUseCase;

    public ProductVariantController(
            CreateProductVariantUseCase createProductVariantUseCase,
            GetProductVariantUseCase getProductVariantUseCase,
            ListProductVariantsUseCase listProductVariantsUseCase,
            UpdateProductVariantUseCase updateProductVariantUseCase,
            ActivateProductVariantUseCase activateProductVariantUseCase,
            DeactivateProductVariantUseCase deactivateProductVariantUseCase) {
        this.createProductVariantUseCase = createProductVariantUseCase;
        this.getProductVariantUseCase = getProductVariantUseCase;
        this.listProductVariantsUseCase = listProductVariantsUseCase;
        this.updateProductVariantUseCase = updateProductVariantUseCase;
        this.activateProductVariantUseCase = activateProductVariantUseCase;
        this.deactivateProductVariantUseCase = deactivateProductVariantUseCase;
    }

    @PostMapping
    public ResponseEntity<ProductVariantRestResponse> create(@RequestBody CreateProductVariantRequest request) {
        ProductVariantRestResponse body = ProductVariantRestResponse.from(createProductVariantUseCase.execute(
                new CreateProductVariantCommand(request.productTypeId(), request.name(), request.description())));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{productVariantId}")
                        .buildAndExpand(body.id())
                        .toUri())
                .body(body);
    }

    @GetMapping
    public List<ProductVariantRestResponse> list(@RequestParam UUID productTypeId) {
        return listProductVariantsUseCase.execute(new ListProductVariantsCommand(productTypeId)).stream()
                .map(ProductVariantRestResponse::from)
                .toList();
    }

    @GetMapping("/{productVariantId}")
    public ProductVariantRestResponse get(@PathVariable UUID productVariantId) {
        return ProductVariantRestResponse.from(
                getProductVariantUseCase.execute(new GetProductVariantCommand(productVariantId)));
    }

    @PutMapping("/{productVariantId}")
    public ProductVariantRestResponse update(
            @PathVariable UUID productVariantId, @RequestBody UpdateProductVariantRequest request) {
        return ProductVariantRestResponse.from(updateProductVariantUseCase.execute(
                new UpdateProductVariantCommand(productVariantId, request.name(), request.description())));
    }

    @PostMapping("/{productVariantId}/activate")
    public ProductVariantRestResponse activate(@PathVariable UUID productVariantId) {
        return ProductVariantRestResponse.from(
                activateProductVariantUseCase.execute(new ActivateProductVariantCommand(productVariantId)));
    }

    @PostMapping("/{productVariantId}/deactivate")
    public ProductVariantRestResponse deactivate(@PathVariable UUID productVariantId) {
        return ProductVariantRestResponse.from(deactivateProductVariantUseCase.execute(
                new DeactivateProductVariantCommand(productVariantId)));
    }
}

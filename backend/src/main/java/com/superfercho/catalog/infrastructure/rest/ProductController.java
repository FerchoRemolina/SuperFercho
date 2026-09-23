package com.superfercho.catalog.infrastructure.rest;

import com.superfercho.catalog.application.dto.ActivateProductCommand;
import com.superfercho.catalog.application.dto.AdjustProductStockCommand;
import com.superfercho.catalog.application.dto.ArchiveProductCommand;
import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.ChangeProductPriceCommand;
import com.superfercho.catalog.application.dto.CreateProductCommand;
import com.superfercho.catalog.application.dto.DeactivateProductCommand;
import com.superfercho.catalog.application.dto.GetProductCommand;
import com.superfercho.catalog.application.dto.ListProductsCommand;
import com.superfercho.catalog.application.dto.RestoreProductCommand;
import com.superfercho.catalog.application.dto.SearchProductsCommand;
import com.superfercho.catalog.application.dto.UpdateProductCommand;
import com.superfercho.catalog.application.usecase.ActivateProductUseCase;
import com.superfercho.catalog.application.usecase.AdjustProductStockUseCase;
import com.superfercho.catalog.application.usecase.ArchiveProductUseCase;
import com.superfercho.catalog.application.usecase.ChangeProductPriceUseCase;
import com.superfercho.catalog.application.usecase.CreateProductUseCase;
import com.superfercho.catalog.application.usecase.DeactivateProductUseCase;
import com.superfercho.catalog.application.usecase.GetProductUseCase;
import com.superfercho.catalog.application.usecase.ListProductsUseCase;
import com.superfercho.catalog.application.usecase.RestoreProductUseCase;
import com.superfercho.catalog.application.usecase.SearchProductsUseCase;
import com.superfercho.catalog.application.usecase.UpdateProductUseCase;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.catalog.infrastructure.rest.dto.AdjustProductStockRequest;
import com.superfercho.catalog.infrastructure.rest.dto.ChangeProductPriceRequest;
import com.superfercho.catalog.infrastructure.rest.dto.CreateProductRequest;
import com.superfercho.catalog.infrastructure.rest.dto.ProductRestResponse;
import com.superfercho.catalog.infrastructure.rest.dto.UpdateProductRequest;
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
@RequestMapping("/api/v1/products")
public class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final GetProductUseCase getProductUseCase;
    private final ListProductsUseCase listProductsUseCase;
    private final SearchProductsUseCase searchProductsUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final ActivateProductUseCase activateProductUseCase;
    private final DeactivateProductUseCase deactivateProductUseCase;
    private final ArchiveProductUseCase archiveProductUseCase;
    private final RestoreProductUseCase restoreProductUseCase;
    private final ChangeProductPriceUseCase changeProductPriceUseCase;
    private final AdjustProductStockUseCase adjustProductStockUseCase;

    public ProductController(
            CreateProductUseCase createProductUseCase,
            GetProductUseCase getProductUseCase,
            ListProductsUseCase listProductsUseCase,
            SearchProductsUseCase searchProductsUseCase,
            UpdateProductUseCase updateProductUseCase,
            ActivateProductUseCase activateProductUseCase,
            DeactivateProductUseCase deactivateProductUseCase,
            ArchiveProductUseCase archiveProductUseCase,
            RestoreProductUseCase restoreProductUseCase,
            ChangeProductPriceUseCase changeProductPriceUseCase,
            AdjustProductStockUseCase adjustProductStockUseCase) {
        this.createProductUseCase = createProductUseCase;
        this.getProductUseCase = getProductUseCase;
        this.listProductsUseCase = listProductsUseCase;
        this.searchProductsUseCase = searchProductsUseCase;
        this.updateProductUseCase = updateProductUseCase;
        this.activateProductUseCase = activateProductUseCase;
        this.deactivateProductUseCase = deactivateProductUseCase;
        this.archiveProductUseCase = archiveProductUseCase;
        this.restoreProductUseCase = restoreProductUseCase;
        this.changeProductPriceUseCase = changeProductPriceUseCase;
        this.adjustProductStockUseCase = adjustProductStockUseCase;
    }

    @PostMapping
    public ResponseEntity<ProductRestResponse> create(@RequestBody CreateProductRequest request) {
        ProductRestResponse body = ProductRestResponse.from(createProductUseCase.execute(new CreateProductCommand(
                request.categoryId(),
                request.barcode(),
                request.name(),
                request.brand(),
                request.description(),
                request.price(),
                request.stock(),
                request.imageUrl())));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{productId}")
                        .buildAndExpand(body.id())
                        .toUri())
                .body(body);
    }

    @GetMapping
    public List<ProductRestResponse> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) CatalogView view) {
        return listProductsUseCase
                .execute(new ListProductsCommand(categoryId, status, catalogView(view)))
                .stream()
                .map(ProductRestResponse::from)
                .toList();
    }

    @GetMapping("/search")
    public List<ProductRestResponse> search(
            @RequestParam(required = false) String text, @RequestParam(required = false) CatalogView view) {
        return searchProductsUseCase.execute(new SearchProductsCommand(text, catalogView(view))).stream()
                .map(ProductRestResponse::from)
                .toList();
    }

    @GetMapping("/{productId}")
    public ProductRestResponse get(
            @PathVariable UUID productId, @RequestParam(required = false) CatalogView view) {
        return ProductRestResponse.from(
                getProductUseCase.execute(new GetProductCommand(productId, catalogView(view))));
    }

    @PutMapping("/{productId}")
    public ProductRestResponse update(
            @PathVariable UUID productId, @RequestBody UpdateProductRequest request) {
        return ProductRestResponse.from(updateProductUseCase.execute(new UpdateProductCommand(
                productId,
                request.productTypeId(),
                request.productVariantId(),
                request.presentation(),
                request.barcode(),
                request.name(),
                request.brand(),
                request.description(),
                request.imageUrl())));
    }

    @PostMapping("/{productId}/activate")
    public ProductRestResponse activate(@PathVariable UUID productId) {
        return ProductRestResponse.from(activateProductUseCase.execute(new ActivateProductCommand(productId)));
    }

    @PostMapping("/{productId}/deactivate")
    public ProductRestResponse deactivate(@PathVariable UUID productId) {
        return ProductRestResponse.from(
                deactivateProductUseCase.execute(new DeactivateProductCommand(productId)));
    }

    @PostMapping("/{productId}/archive")
    public ProductRestResponse archive(@PathVariable UUID productId) {
        return ProductRestResponse.from(archiveProductUseCase.execute(new ArchiveProductCommand(productId)));
    }

    @PostMapping("/{productId}/restore")
    public ProductRestResponse restore(@PathVariable UUID productId) {
        return ProductRestResponse.from(restoreProductUseCase.execute(new RestoreProductCommand(productId)));
    }

    @PostMapping("/{productId}/price")
    public ProductRestResponse changePrice(
            @PathVariable UUID productId, @RequestBody ChangeProductPriceRequest request) {
        return ProductRestResponse.from(
                changeProductPriceUseCase.execute(new ChangeProductPriceCommand(productId, request.price())));
    }

    @PostMapping("/{productId}/stock")
    public ProductRestResponse adjustStock(
            @PathVariable UUID productId, @RequestBody AdjustProductStockRequest request) {
        return ProductRestResponse.from(adjustProductStockUseCase.execute(
                new AdjustProductStockCommand(productId, request.stock())));
    }

    private static CatalogView catalogView(CatalogView view) {
        return view == null ? CatalogView.PUBLIC : view;
    }
}

package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.QuantityEquivalenceCommand;
import com.superfercho.catalog.application.dto.QuantityEquivalenceResult;
import com.superfercho.catalog.application.dto.QuantityEquivalenceStatus;
import com.superfercho.catalog.application.dto.QuantityPackageOption;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.domain.exception.InvalidPresentationException;
import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.PackageQuantityEquivalence;
import com.superfercho.catalog.domain.model.Presentation;
import com.superfercho.catalog.domain.model.PresentationUnit;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.domain.model.ProductVariant;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.util.Objects;

public final class ResolveProductQuantityEquivalenceUseCase {

    static final String AVAILABLE = "AVAILABLE";
    static final String INSUFFICIENT_STOCK = "INSUFFICIENT_STOCK";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductTypeRepository productTypeRepository;
    private final ProductVariantRepository productVariantRepository;

    public ResolveProductQuantityEquivalenceUseCase(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductTypeRepository productTypeRepository,
            ProductVariantRepository productVariantRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productTypeRepository = productTypeRepository;
        this.productVariantRepository = productVariantRepository;
    }

    public QuantityEquivalenceResult execute(QuantityEquivalenceCommand command) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(command.productId(), "productId");
        Presentation requested = requestedPresentation(command.quantity(), command.unit());
        Product product = productRepository.findById(command.productId()).orElse(null);
        if (product == null) {
            return QuantityEquivalenceResult.notFound(command.productId());
        }
        if (!isPubliclyVisible(product)) {
            return QuantityEquivalenceResult.notSellable(
                    product.id(), product.name(), product.presentation(), requested.quantity(), requested.unit());
        }
        Money price = product.price();
        if (price == null) {
            return QuantityEquivalenceResult.priceUnavailable(
                    product.id(),
                    product.name(),
                    product.presentation(),
                    requested.quantity(),
                    requested.unit(),
                    product.stock());
        }
        PackageQuantityEquivalence.Match match =
                PackageQuantityEquivalence.match(product.presentation(), requested);
        if (!match.compatible()) {
            return incompatible(product, price, requested);
        }
        if (match.exactMatch()) {
            return exact(product, price, requested, match);
        }
        return inexact(product, price, requested, match);
    }

    private boolean isPubliclyVisible(Product product) {
        Category category = categoryRepository.findById(product.categoryId()).orElse(null);
        ProductType productType =
                productTypeRepository.findById(product.productTypeId()).orElse(null);
        ProductVariant productVariant = product.productVariantId() == null
                ? null
                : productVariantRepository.findById(product.productVariantId()).orElse(null);
        return CatalogVisibility.isPubliclyVisible(product, category, productType, productVariant);
    }

    private static Presentation requestedPresentation(BigDecimal quantity, PresentationUnit unit) {
        if (quantity == null || unit == null) {
            throw new InvalidPresentationException("requested quantity and unit are required");
        }
        return Presentation.of(quantity, unit);
    }

    private static QuantityEquivalenceResult exact(
            Product product, Money price, Presentation requested, PackageQuantityEquivalence.Match match) {
        QuantityPackageOption exact = option(match.exact(), product.stock());
        String availability = exact.availability();
        return new QuantityEquivalenceResult(
                QuantityEquivalenceStatus.EXACT,
                "EXACT MATCH. This is not a cart mutation. Communicate the package count and wait for explicit acceptance of that package count before add_cart_item.",
                product.id(),
                product.name(),
                product.presentation(),
                price,
                match.baseUnit(),
                match.packagePhysicalQuantityBase(),
                requested.quantity(),
                requested.unit(),
                match.requestedPhysicalQuantityBase(),
                true,
                exact.packageCount(),
                exact.physicalTotalBase(),
                null,
                null,
                product.stock(),
                true,
                availability,
                exact.unavailabilityReason());
    }

    private static QuantityEquivalenceResult inexact(
            Product product, Money price, Presentation requested, PackageQuantityEquivalence.Match match) {
        QuantityPackageOption lower =
                match.lowerAlternative() == null ? null : option(match.lowerAlternative(), product.stock());
        QuantityPackageOption upper =
                match.upperAlternative() == null ? null : option(match.upperAlternative(), product.stock());
        return new QuantityEquivalenceResult(
                QuantityEquivalenceStatus.INEXACT,
                "NO EXACT MATCH. Alternatives are whole packages, not a purchase. Do not call add_cart_item until the customer chooses a package count. A generic yes is not a choice.",
                product.id(),
                product.name(),
                product.presentation(),
                price,
                match.baseUnit(),
                match.packagePhysicalQuantityBase(),
                requested.quantity(),
                requested.unit(),
                match.requestedPhysicalQuantityBase(),
                false,
                null,
                null,
                lower,
                upper,
                product.stock(),
                true,
                null,
                null);
    }

    private static QuantityEquivalenceResult incompatible(Product product, Money price, Presentation requested) {
        return new QuantityEquivalenceResult(
                QuantityEquivalenceStatus.INCOMPATIBLE_UNITS,
                "INCOMPATIBLE UNITS. Do not invent a conversion. This SKU is sold by its catalog presentation, not by the requested unit.",
                product.id(),
                product.name(),
                product.presentation(),
                price,
                null,
                null,
                requested.quantity(),
                requested.unit(),
                null,
                false,
                null,
                null,
                null,
                null,
                product.stock(),
                true,
                null,
                "INCOMPATIBLE_UNITS");
    }

    private static QuantityPackageOption option(PackageQuantityEquivalence.Option match, int stock) {
        boolean enough = stock >= match.packageCount();
        return new QuantityPackageOption(
                match.packageCount(),
                match.physicalTotalBase(),
                match.baseUnit(),
                match.differenceFromRequestedBase(),
                enough ? AVAILABLE : INSUFFICIENT_STOCK,
                enough ? null : "catalog stock is " + stock);
    }
}

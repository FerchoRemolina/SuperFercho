package com.superfercho.platform.demo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.superfercho.catalog.application.dto.ArchiveProductCommand;
import com.superfercho.catalog.application.dto.CategoryResult;
import com.superfercho.catalog.domain.model.CategoryIcon;
import com.superfercho.catalog.application.dto.CreateCategoryCommand;
import com.superfercho.catalog.application.dto.CreateProductCommand;
import com.superfercho.catalog.application.dto.CreateProductVariantCommand;
import com.superfercho.catalog.application.dto.DeactivateProductCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.dto.ProductVariantResult;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.application.usecase.ArchiveProductUseCase;
import com.superfercho.catalog.application.usecase.CreateCategoryUseCase;
import com.superfercho.catalog.application.usecase.CreateProductUseCase;
import com.superfercho.catalog.application.usecase.CreateProductVariantUseCase;
import com.superfercho.catalog.application.usecase.DeactivateProductUseCase;
import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.Presentation;
import com.superfercho.catalog.domain.model.PresentationUnit;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.domain.model.ProductTypeStatus;
import com.superfercho.catalog.domain.model.ProductVariant;
import com.superfercho.knowledge.application.dto.CreateDocumentCommand;
import com.superfercho.knowledge.application.dto.DocumentResult;
import com.superfercho.knowledge.application.dto.ListDocumentsCommand;
import com.superfercho.knowledge.application.dto.ProcessDocumentCommand;
import com.superfercho.knowledge.application.port.KnowledgeDocumentRepository;
import com.superfercho.knowledge.application.usecase.CreateDocumentUseCase;
import com.superfercho.knowledge.application.usecase.ListDocumentsUseCase;
import com.superfercho.knowledge.application.usecase.ProcessDocumentUseCase;
import com.superfercho.platform.money.Money;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Local-profile base catalog and knowledge documents. Separate from Flyway schema
 * migrations. Idempotent: seeds only missing dataset product identities; skips when all
 * loadable products are already present. Knowledge seed skips when documents already
 * exist. Document processing is best-effort (requires OpenAI when configured).
 *
 * <p>Catalog seed reads {@code docs/dataset/superfercho-dataset-156.json}. Products whose
 * presentation cannot be represented by {@link Presentation}/{@link PresentationUnit} are
 * skipped and logged as pending.
 */
@Component
@Profile("local")
@Order(200)
public class LocalDemoDataRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalDemoDataRunner.class);

    static final String DATASET_RELATIVE_PATH = "docs/dataset/superfercho-dataset-156.json";
    static final int DATASET_DEFINED_COUNT = 156;
    static final int LOADABLE_COUNT = 156;
    static final int PENDING_COUNT = 0;

    /** Presentations that the current domain model cannot represent exactly. */
    static final Set<String> PENDING_PRESENTATIONS = Set.of("Manojo", "3 × 90 g", "3 × 110 g");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Pattern DISCRETE_COUNT =
            Pattern.compile("^(\\d+)\\s+(unidades|rollos)$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern MEASURE =
            Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s*(ml|kg|g|l)$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final ProductRepository productRepository;
    private final ProductTypeRepository productTypeRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final CreateCategoryUseCase createCategoryUseCase;
    private final CreateProductUseCase createProductUseCase;
    private final CreateProductVariantUseCase createProductVariantUseCase;
    private final DeactivateProductUseCase deactivateProductUseCase;
    private final ArchiveProductUseCase archiveProductUseCase;
    private final CreateDocumentUseCase createDocumentUseCase;
    private final ListDocumentsUseCase listDocumentsUseCase;
    private final ProcessDocumentUseCase processDocumentUseCase;
    private final Clock clock;
    private final boolean enabled;

    public LocalDemoDataRunner(
            ProductRepository productRepository,
            ProductTypeRepository productTypeRepository,
            CategoryRepository categoryRepository,
            ProductVariantRepository productVariantRepository,
            KnowledgeDocumentRepository documentRepository,
            CreateCategoryUseCase createCategoryUseCase,
            CreateProductUseCase createProductUseCase,
            CreateProductVariantUseCase createProductVariantUseCase,
            DeactivateProductUseCase deactivateProductUseCase,
            ArchiveProductUseCase archiveProductUseCase,
            CreateDocumentUseCase createDocumentUseCase,
            ListDocumentsUseCase listDocumentsUseCase,
            ProcessDocumentUseCase processDocumentUseCase,
            Clock clock,
            @Value("${superfercho.dev.demo-seed.enabled:true}") boolean enabled) {
        this.productRepository = productRepository;
        this.productTypeRepository = productTypeRepository;
        this.categoryRepository = categoryRepository;
        this.productVariantRepository = productVariantRepository;
        this.documentRepository = documentRepository;
        this.createCategoryUseCase = createCategoryUseCase;
        this.createProductUseCase = createProductUseCase;
        this.createProductVariantUseCase = createProductVariantUseCase;
        this.deactivateProductUseCase = deactivateProductUseCase;
        this.archiveProductUseCase = archiveProductUseCase;
        this.createDocumentUseCase = createDocumentUseCase;
        this.listDocumentsUseCase = listDocumentsUseCase;
        this.processDocumentUseCase = processDocumentUseCase;
        this.clock = clock;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            LOGGER.info("Local demo seed skipped: superfercho.dev.demo-seed.enabled=false");
            return;
        }
        seedCatalogIfEmpty();
        seedKnowledgeIfEmpty();
    }

    void seedCatalogIfEmpty() {
        List<DatasetProduct> dataset = loadDatasetProducts();
        if (dataset.size() != DATASET_DEFINED_COUNT) {
            throw new IllegalStateException(
                    "Expected " + DATASET_DEFINED_COUNT + " dataset products, found " + dataset.size());
        }

        List<DatasetProduct> pending = dataset.stream().filter(p -> !isLoadable(p)).toList();
        List<DatasetProduct> loadable = dataset.stream().filter(LocalDemoDataRunner::isLoadable).toList();
        if (loadable.size() != LOADABLE_COUNT || pending.size() != PENDING_COUNT) {
            throw new IllegalStateException(
                    "Expected " + LOADABLE_COUNT + " loadable and " + PENDING_COUNT + " pending products, found "
                            + loadable.size() + " / " + pending.size());
        }

        List<Product> existing = productRepository.findAll();
        Map<UUID, String> variantNames = loadVariantNames(existing);
        List<DatasetProduct> missing = loadable.stream()
                .filter(expected -> existing.stream()
                        .noneMatch(product -> matchesDatasetProduct(
                                expected, product, variantNames.get(product.productVariantId()))))
                .toList();

        if (missing.isEmpty()) {
            LOGGER.info("Local base catalog already present; skipping catalog seed");
            return;
        }

        if (!existing.isEmpty()) {
            LOGGER.info(
                    "Local base catalog incomplete ({} present, {} missing dataset identities); seeding missing products",
                    existing.size(),
                    missing.size());
        }

        Map<String, List<DatasetProduct>> byCategory = new LinkedHashMap<>();
        for (DatasetProduct product : missing) {
            byCategory.computeIfAbsent(product.category(), ignored -> new ArrayList<>()).add(product);
        }

        Map<String, CategoryTypeIds> categoryTypeCache = new HashMap<>();
        Map<String, UUID> variantCache = new HashMap<>();
        int productCount = 0;
        int inactiveCount = 0;
        int archivedCount = 0;
        for (Map.Entry<String, List<DatasetProduct>> entry : byCategory.entrySet()) {
            CategoryTypeIds categoryType = resolveCategoryAndType(entry.getKey(), categoryTypeCache);
            for (DatasetProduct product : entry.getValue()) {
                ProductResult created = createProduct(categoryType.productTypeId(), product, variantCache);
                applyStatus(created.id(), product.status());
                if ("INACTIVE".equals(product.status())) {
                    inactiveCount++;
                } else if ("ARCHIVED".equals(product.status())) {
                    archivedCount++;
                }
                productCount++;
            }
        }

        LOGGER.info(
                "Local base catalog seeded ({} categories touched, {} products; {} inactive, {} archived). Pending non-representable presentations ({}): {}",
                byCategory.size(),
                productCount,
                inactiveCount,
                archivedCount,
                pending.size(),
                pending.stream().map(p -> p.id() + ":" + p.presentation()).toList());
    }

    void seedKnowledgeIfEmpty() {
        if (!documentRepository.findAll().isEmpty()) {
            LOGGER.info("Local demo knowledge already present; skipping knowledge seed");
            return;
        }

        List<CreateDocumentCommand> documents = List.of(
                new CreateDocumentCommand(
                        "Horarios de atención",
                        "faq",
                        """
                                SuperFercho atiende pedidos en línea todos los días.
                                El servicio de preparación de pedidos funciona de 8:00 a 20:00 hora de Colombia.
                                Los pedidos realizados fuera de ese horario quedan registrados y se preparan al siguiente día hábil de servicio.
                                """),
                new CreateDocumentCommand(
                        "Métodos de pago",
                        "faq",
                        """
                                En SuperFercho puedes pagar con tarjeta simulada o contra entrega.
                                La tarjeta simulada aprueba el pago al confirmar el pedido; no se solicitan datos reales de tarjeta bancaria.
                                El pago contra entrega queda pendiente hasta que recibas el pedido.
                                """),
                new CreateDocumentCommand(
                        "Política de cancelación",
                        "faq",
                        """
                                Puedes cancelar un pedido durante los primeros 15 minutos después de realizarlo, mientras esté pendiente.
                                Pasado ese plazo el pedido se confirma automáticamente y ya no se puede cancelar desde la cuenta del cliente.
                                Si el pago con tarjeta simulada estaba aprobado, al cancelar a tiempo se registra el reembolso sin cambiar el estado del pago a otro valor.
                                """),
                new CreateDocumentCommand(
                        "Entregas y preparación",
                        "faq",
                        """
                                Después de confirmar el pedido, el supermercado lo prepara y marca los estados: confirmado, en preparación, listo y entregado.
                                La dirección de envío queda guardada en el pedido y no cambia si luego editas tus direcciones guardadas.
                                """),
                new CreateDocumentCommand(
                        "Catálogo y disponibilidad",
                        "faq",
                        """
                                Solo se venden productos activos cuya categoría también esté activa.
                                Un producto con existencias en cero aparece como agotado: puedes verlo, pero no debes agregarlo al carrito para comprarlo hasta que haya stock.
                                Los precios del carrito al agregar son informativos; al confirmar el pedido se usa el precio vigente del catálogo.
                                """));

        for (CreateDocumentCommand command : documents) {
            DocumentResult created = createDocumentUseCase.execute(command);
            tryProcess(created.id(), created.title());
        }

        LOGGER.info(
                "Local demo knowledge seeded ({} documents). Process from Admin if embeddings were not ready.",
                listDocumentsUseCase.execute(new ListDocumentsCommand()).size());
    }

    private CategoryIcon iconFor(String categoryName) {
        String normalizedName = java.text.Normalizer.normalize(categoryName, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
        if (normalizedName.contains("aseo") || normalizedName.contains("limpieza") || normalizedName.contains("hogar")) {
            return CategoryIcon.CLEANING;
        }
        if (normalizedName.contains("bebida")) {
            return CategoryIcon.DRINKS;
        }
        if (normalizedName.contains("higiene") || normalizedName.contains("cuidado personal")) {
            return CategoryIcon.PERSONAL_CARE;
        }
        if (normalizedName.contains("despensa") || normalizedName.contains("granos") || normalizedName.contains("basicos")) {
            return CategoryIcon.GROCERY;
        }
        if (normalizedName.contains("frutas") || normalizedName.contains("verduras")) {
            return CategoryIcon.FRUITS;
        }
        if (normalizedName.contains("panadera") || normalizedName.contains("pastelera")) {
            return CategoryIcon.BAKERY;
        }
        if (normalizedName.contains("carnes") || normalizedName.contains("aves") || normalizedName.contains("pescados")) {
            return CategoryIcon.MEAT;
        }
        if (normalizedName.contains("lacteos") || normalizedName.contains("huevos") || normalizedName.contains("refrigerados")) {
            return CategoryIcon.DAIRY;
        }
        return CategoryIcon.OTHER;
    }

    private UUID createCategory(String name, String description, CategoryIcon icon) {
        CategoryResult created =
                createCategoryUseCase.execute(new CreateCategoryCommand(name, description, icon.name()));
        return created.id();
    }

    private CategoryTypeIds resolveCategoryAndType(String categoryName, Map<String, CategoryTypeIds> cache) {
        CategoryTypeIds cached = cache.get(categoryName);
        if (cached != null) {
            return cached;
        }

        Category existingCategory = categoryRepository.findAll().stream()
                .filter(category -> category.name().equalsIgnoreCase(categoryName))
                .findFirst()
                .orElse(null);

        UUID categoryId;
        UUID productTypeId;
        if (existingCategory != null) {
            categoryId = existingCategory.id();
            List<ProductType> types = productTypeRepository.findByCategoryId(categoryId);
            productTypeId = types.stream()
                    .filter(type -> type.name().equalsIgnoreCase(categoryName))
                    .map(ProductType::id)
                    .findFirst()
                    .orElseGet(() -> types.isEmpty()
                            ? createProductType(categoryId, categoryName)
                            : types.getFirst().id());
        } else {
            categoryId = createCategory(categoryName, null, iconFor(categoryName));
            productTypeId = createProductType(categoryId, categoryName);
        }

        CategoryTypeIds resolved = new CategoryTypeIds(categoryId, productTypeId);
        cache.put(categoryName, resolved);
        return resolved;
    }

    private UUID createProductType(UUID categoryId, String categoryName) {
        Instant now = clock.instant();
        ProductType saved = productTypeRepository.save(ProductType.create(
                UUID.randomUUID(),
                categoryId,
                categoryName,
                "Tipo base local para " + categoryName,
                ProductTypeStatus.ACTIVE,
                now,
                now));
        return saved.id();
    }

    private ProductResult createProduct(
            UUID productTypeId, DatasetProduct product, Map<String, UUID> variantCache) {
        UUID productVariantId = resolveVariantId(productTypeId, product.variant(), variantCache);
        return createProductUseCase.execute(new CreateProductCommand(
                productTypeId,
                productVariantId,
                parsePresentation(product.presentation()),
                product.barcode(),
                product.name(),
                product.brand(),
                product.description(),
                Money.cop(product.price()),
                product.stock(),
                product.imageUrl()));
    }

    private UUID resolveVariantId(UUID productTypeId, String variantName, Map<String, UUID> variantCache) {
        if (variantName == null) {
            return null;
        }
        String cacheKey = productTypeId + "|" + variantName;
        UUID cached = variantCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        UUID existingId = productVariantRepository.findByProductTypeId(productTypeId).stream()
                .filter(variant -> normalize(variant.name()).equals(normalize(variantName)))
                .map(ProductVariant::id)
                .findFirst()
                .orElse(null);
        if (existingId != null) {
            variantCache.put(cacheKey, existingId);
            return existingId;
        }
        ProductVariantResult created = createProductVariantUseCase.execute(
                new CreateProductVariantCommand(productTypeId, variantName, null));
        variantCache.put(cacheKey, created.id());
        return created.id();
    }

    private Map<UUID, String> loadVariantNames(List<Product> products) {
        return productVariantRepository
                .findByIds(products.stream()
                        .map(Product::productVariantId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(ProductVariant::id, ProductVariant::name));
    }

    private void applyStatus(UUID productId, String status) {
        Objects.requireNonNull(status, "status");
        switch (status) {
            case "ACTIVE" -> {
                // CreateProductUseCase always creates ACTIVE.
            }
            case "INACTIVE" -> deactivateProductUseCase.execute(new DeactivateProductCommand(productId));
            case "ARCHIVED" -> archiveProductUseCase.execute(new ArchiveProductCommand(productId));
            default -> throw new IllegalStateException("Unsupported dataset status: " + status);
        }
    }

    private void tryProcess(UUID documentId, String title) {
        try {
            processDocumentUseCase.execute(new ProcessDocumentCommand(documentId));
            LOGGER.info("Processed demo knowledge document: {}", title);
        } catch (RuntimeException ex) {
            LOGGER.warn(
                    "Demo knowledge document '{}' left unprocessed ({}). Process it from Admin when OpenAI is configured.",
                    title,
                    ex.getMessage());
        }
    }

    static boolean isLoadable(DatasetProduct product) {
        return product != null && !PENDING_PRESENTATIONS.contains(product.presentation());
    }

    static List<DatasetProduct> pendingProducts(List<DatasetProduct> dataset) {
        return dataset.stream().filter(p -> !isLoadable(p)).toList();
    }

    static List<DatasetProduct> loadableProducts(List<DatasetProduct> dataset) {
        return dataset.stream().filter(LocalDemoDataRunner::isLoadable).toList();
    }

    /**
     * Same identity rules as {@code HistoricalDemoProductIndex}: barcode when present,
     * otherwise name + brand + price + description/variant.
     */
    public static boolean matchesDatasetProduct(DatasetProduct expected, Product actual, String variantName) {
        if (hasText(expected.barcode())) {
            return expected.barcode().equals(actual.barcode());
        }
        if (!normalize(expected.name()).equals(normalize(actual.name()))) {
            return false;
        }
        if (!normalizeNullable(expected.brand()).equals(normalizeNullable(actual.brand()))) {
            return false;
        }
        if (expected.price().compareTo(actual.price().amount()) != 0) {
            return false;
        }
        if (hasText(expected.description()) || hasText(actual.description())) {
            return normalizeNullable(expected.description()).equals(normalizeNullable(actual.description()));
        }
        if (hasText(expected.variant())) {
            return normalize(expected.variant()).equals(normalizeNullable(variantName));
        }
        return !hasText(variantName);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? "" : normalize(value);
    }

    public static List<DatasetProduct> loadDatasetProducts() {
        Path path = resolveDatasetPath();
        try {
            return OBJECT_MAPPER.readValue(Files.readString(path), new TypeReference<>() {});
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read demo dataset: " + path, ex);
        }
    }

    static Path resolveDatasetPath() {
        Path start = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (Path dir = start; dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve(DATASET_RELATIVE_PATH);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Demo dataset not found: " + DATASET_RELATIVE_PATH + " (searched from " + start + ")");
    }

    /**
     * Strict parser: only quantity + {@link PresentationUnit} combinations supported by the domain.
     * Does not invent equivalences for pending presentations such as {@code Manojo} or multipacks.
     */
    static Presentation parsePresentation(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("presentation cannot be null or blank");
        }
        String value = raw.trim();
        if (PENDING_PRESENTATIONS.contains(value)) {
            throw new IllegalArgumentException("presentation is not representable: " + value);
        }
        if ("Unidad".equals(value)) {
            return Presentation.of(1, PresentationUnit.UNIT);
        }

        Matcher discrete = DISCRETE_COUNT.matcher(value);
        if (discrete.matches()) {
            int quantity = Integer.parseInt(discrete.group(1));
            String unit = discrete.group(2).toLowerCase(Locale.ROOT);
            return switch (unit) {
                case "unidades" -> Presentation.of(quantity, PresentationUnit.UNIT);
                case "rollos" -> Presentation.of(quantity, PresentationUnit.ROLL);
                default -> throw new IllegalArgumentException("unsupported discrete presentation: " + value);
            };
        }

        Matcher measure = MEASURE.matcher(value);
        if (measure.matches()) {
            BigDecimal quantity = new BigDecimal(measure.group(1));
            String unit = measure.group(2).toLowerCase(Locale.ROOT);
            PresentationUnit presentationUnit = switch (unit) {
                case "ml" -> PresentationUnit.ML;
                case "kg" -> PresentationUnit.KG;
                case "g" -> PresentationUnit.G;
                case "l" -> PresentationUnit.L;
                default -> throw new IllegalArgumentException("unsupported measure unit: " + value);
            };
            return Presentation.of(quantity, presentationUnit);
        }

        throw new IllegalArgumentException("unsupported presentation: " + value);
    }

    private record CategoryTypeIds(UUID categoryId, UUID productTypeId) {
    }

    public record DatasetProduct(
            String category,
            String family,
            String name,
            String brand,
            String variant,
            String presentation,
            String description,
            BigDecimal price,
            int stock,
            String status,
            String barcode,
            String imageUrl,
            String id) {
    }
}

package com.superfercho.platform.demo;

import com.superfercho.catalog.application.dto.CategoryResult;
import com.superfercho.catalog.application.dto.CreateCategoryCommand;
import com.superfercho.catalog.application.dto.CreateProductCommand;
import com.superfercho.catalog.application.dto.DeactivateCategoryCommand;
import com.superfercho.catalog.application.dto.DeactivateProductCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.usecase.CreateCategoryUseCase;
import com.superfercho.catalog.application.usecase.CreateProductUseCase;
import com.superfercho.catalog.application.usecase.DeactivateCategoryUseCase;
import com.superfercho.catalog.application.usecase.DeactivateProductUseCase;
import com.superfercho.knowledge.application.dto.CreateDocumentCommand;
import com.superfercho.knowledge.application.dto.DocumentResult;
import com.superfercho.knowledge.application.dto.ListDocumentsCommand;
import com.superfercho.knowledge.application.dto.ProcessDocumentCommand;
import com.superfercho.knowledge.application.port.KnowledgeDocumentRepository;
import com.superfercho.knowledge.application.usecase.CreateDocumentUseCase;
import com.superfercho.knowledge.application.usecase.ListDocumentsUseCase;
import com.superfercho.knowledge.application.usecase.ProcessDocumentUseCase;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Local-profile demo catalog and knowledge documents. Separate from Flyway schema
 * migrations. Idempotent: skips when catalog products or knowledge documents already
 * exist. Document processing is best-effort (requires OpenAI when configured).
 */
@Component
@Profile("local")
@Order(200)
public class LocalDemoDataRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalDemoDataRunner.class);

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final CreateCategoryUseCase createCategoryUseCase;
    private final CreateProductUseCase createProductUseCase;
    private final DeactivateProductUseCase deactivateProductUseCase;
    private final DeactivateCategoryUseCase deactivateCategoryUseCase;
    private final CreateDocumentUseCase createDocumentUseCase;
    private final ListDocumentsUseCase listDocumentsUseCase;
    private final ProcessDocumentUseCase processDocumentUseCase;
    private final boolean enabled;

    public LocalDemoDataRunner(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            KnowledgeDocumentRepository documentRepository,
            CreateCategoryUseCase createCategoryUseCase,
            CreateProductUseCase createProductUseCase,
            DeactivateProductUseCase deactivateProductUseCase,
            DeactivateCategoryUseCase deactivateCategoryUseCase,
            CreateDocumentUseCase createDocumentUseCase,
            ListDocumentsUseCase listDocumentsUseCase,
            ProcessDocumentUseCase processDocumentUseCase,
            @Value("${superfercho.dev.demo-seed.enabled:true}") boolean enabled) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.documentRepository = documentRepository;
        this.createCategoryUseCase = createCategoryUseCase;
        this.createProductUseCase = createProductUseCase;
        this.deactivateProductUseCase = deactivateProductUseCase;
        this.deactivateCategoryUseCase = deactivateCategoryUseCase;
        this.createDocumentUseCase = createDocumentUseCase;
        this.listDocumentsUseCase = listDocumentsUseCase;
        this.processDocumentUseCase = processDocumentUseCase;
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
        if (!productRepository.findAll().isEmpty()) {
            LOGGER.info("Local demo catalog already present; skipping catalog seed");
            return;
        }

        Map<String, UUID> categories = new LinkedHashMap<>();
        categories.put(
                "frutas",
                createCategory("Frutas y verduras", "Frutas frescas, verduras y hierbas."));
        categories.put("lacteos", createCategory("Lácteos", "Leche, yogurt, quesos y derivados."));
        categories.put("carnes", createCategory("Carnes", "Res, pollo, cerdo y embutidos."));
        categories.put("bebidas", createCategory("Bebidas", "Aguas, jugos, gaseosas y café."));
        categories.put("despensa", createCategory("Despensa", "Arroz, pasta, aceites y granos."));
        categories.put("snacks", createCategory("Snacks", "Pasabocas y dulces."));
        categories.put("aseo", createCategory("Aseo del hogar", "Limpieza y papelería del hogar."));
        categories.put(
                "cuidado", createCategory("Cuidado personal", "Higiene y cuidado diario."));
        categories.put(
                "temporada",
                createCategory("Temporada especial", "Categoría de demostración (se desactiva)."));

        createProduct(
                categories.get("frutas"),
                "7702001001001",
                "Banano Urabá",
                "Fruver del Campo",
                "Racimo de banano fresco, ideal para el desayuno.",
                "3200.00",
                40,
                "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?auto=format&fit=crop&w=800&q=80");
        createProduct(
                categories.get("frutas"),
                "7702001001002",
                "Tomate chonto",
                "Fruver del Campo",
                "Tomate fresco para ensaladas y guisos.",
                "4500.00",
                25,
                "https://images.unsplash.com/photo-1546470427-e26264be0b27?auto=format&fit=crop&w=800&q=80");
        createProduct(
                categories.get("lacteos"),
                "7702001002001",
                "Leche entera 1 L",
                "Colanta",
                "Leche entera pasteurizada en bolsa de un litro.",
                "4200.00",
                60,
                "https://images.unsplash.com/photo-1563636619-e9143da7973b?auto=format&fit=crop&w=800&q=80");
        createProduct(
                categories.get("lacteos"),
                "7702001002002",
                "Yogurt natural 1 L",
                "Alpina",
                "Yogurt natural sin azúcar añadida.",
                "7800.00",
                0,
                "https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=800&q=80");
        createProduct(
                categories.get("carnes"),
                "7702001003001",
                "Pechuga de pollo",
                "Friko",
                "Pechuga de pollo deshuesada, bandeja de 500 g.",
                "12500.00",
                18,
                "https://images.unsplash.com/photo-1604503468506-a8da13d82791?auto=format&fit=crop&w=800&q=80");
        createProduct(
                categories.get("bebidas"),
                "7702001004001",
                "Agua sin gas 600 ml",
                "Cristal",
                "Botella de agua sin gas lista para llevar.",
                "1800.00",
                80,
                "https://images.unsplash.com/photo-1548839140-46a3dd4b1b0c?auto=format&fit=crop&w=800&q=80");
        createProduct(
                categories.get("bebidas"),
                "7702001004002",
                "Café molido 500 g",
                "Juan Valdez",
                "Café molido de origen colombiano.",
                "18900.00",
                22,
                "https://images.unsplash.com/photo-1447933601403-0c6688de566e?auto=format&fit=crop&w=800&q=80");
        createProduct(
                categories.get("despensa"),
                "7702001005001",
                "Arroz blanco 1 kg",
                "Diana",
                "Arroz de grano largo para el día a día.",
                "5200.00",
                45,
                "https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=800&q=80");
        createProduct(
                categories.get("despensa"),
                "7702001005002",
                "Aceite vegetal 1 L",
                "Team",
                "Aceite vegetal para freír y cocinar.",
                "9800.00",
                30,
                "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?auto=format&fit=crop&w=800&q=80");
        createProduct(
                categories.get("snacks"),
                "7702001006001",
                "Papas limón 150 g",
                "Margarita",
                "Pasabocas de papa sabor limón.",
                "3500.00",
                50,
                "https://images.unsplash.com/photo-1566478989037-eec170784d0b?auto=format&fit=crop&w=800&q=80");
        createProduct(
                categories.get("aseo"),
                "7702001007001",
                "Detergente líquido 1 L",
                "Fab",
                "Detergente líquido para ropa de color.",
                "14500.00",
                20,
                "https://images.unsplash.com/photo-1585421514738-01798e348b17?auto=format&fit=crop&w=800&q=80");
        createProduct(
                categories.get("cuidado"),
                "7702001008001",
                "Jabón antibacterial",
                "Protex",
                "Jabón de tocador antibacterial en barra.",
                "2900.00",
                35,
                "https://images.unsplash.com/photo-1556228578-0d85b1a4d571?auto=format&fit=crop&w=800&q=80");

        ProductResult inactiveProduct = createProduct(
                categories.get("snacks"),
                "7702001006002",
                "Galletas surtidas descontinuadas",
                "SuperFercho",
                "Producto de demostración inactivo; no aparece en el catálogo público.",
                "1000.00",
                5,
                "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?auto=format&fit=crop&w=800&q=80");
        deactivateProductUseCase.execute(new DeactivateProductCommand(inactiveProduct.id()));

        ProductResult seasonal = createProduct(
                categories.get("temporada"),
                "7702001009001",
                "Canasta temporada",
                "SuperFercho",
                "Producto de demostración ligado a una categoría inactiva.",
                "25000.00",
                3,
                "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=800&q=80");
        deactivateCategoryUseCase.execute(new DeactivateCategoryCommand(categories.get("temporada")));

        LOGGER.info(
                "Local demo catalog seeded ({} categories, products including stock=0 and inactive cases). Seasonal product id={}",
                categoryRepository.findAll().size(),
                seasonal.id());
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

    private UUID createCategory(String name, String description) {
        CategoryResult created =
                createCategoryUseCase.execute(new CreateCategoryCommand(name, description));
        return created.id();
    }

    private ProductResult createProduct(
            UUID categoryId,
            String barcode,
            String name,
            String brand,
            String description,
            String price,
            int stock,
            String imageUrl) {
        return createProductUseCase.execute(new CreateProductCommand(
                categoryId,
                barcode,
                name,
                brand,
                description,
                Money.cop(new BigDecimal(price)),
                stock,
                imageUrl));
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
}

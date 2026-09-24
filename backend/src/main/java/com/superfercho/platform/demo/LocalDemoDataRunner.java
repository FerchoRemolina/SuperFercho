package com.superfercho.platform.demo;

import com.superfercho.catalog.application.dto.CategoryResult;
import com.superfercho.catalog.application.dto.CreateCategoryCommand;
import com.superfercho.catalog.application.dto.CreateProductCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.application.usecase.CreateCategoryUseCase;
import com.superfercho.catalog.application.usecase.CreateProductUseCase;
import com.superfercho.catalog.domain.model.Presentation;
import com.superfercho.catalog.domain.model.PresentationUnit;
import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.domain.model.ProductTypeStatus;
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
import java.time.Clock;
import java.time.Instant;
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
 * Local-profile base catalog and knowledge documents. Separate from Flyway schema
 * migrations. Idempotent: skips when catalog products or knowledge documents already
 * exist. Document processing is best-effort (requires OpenAI when configured).
 */
@Component
@Profile("local")
@Order(200)
public class LocalDemoDataRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalDemoDataRunner.class);

    private static final List<CategorySeed> CATEGORIES = List.of(
            new CategorySeed(
                    "frutas",
                    "Frutas y verduras",
                    "Frutas frescas, verduras y hierbas del día.",
                    List.of(
                            product(
                                    "7702101001001",
                                    "Banano Urabá",
                                    "Fruver del Campo",
                                    "Racimo de banano fresco, ideal para el desayuno.",
                                    "3200.00",
                                    48,
                                    "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101001002",
                                    "Tomate chonto",
                                    "Fruver del Campo",
                                    "Tomate fresco para ensaladas y guisos.",
                                    "4500.00",
                                    36,
                                    "https://images.unsplash.com/photo-1546470427-e26264be0b27?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101001003",
                                    "Aguacate Hass",
                                    "Fruver del Campo",
                                    "Aguacate maduro, unidad aproximada 250 g.",
                                    "3900.00",
                                    28,
                                    "https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101001004",
                                    "Papa pastusa 1 kg",
                                    "Fruver del Campo",
                                    "Papa pastusa lavada para sopas y frituras.",
                                    "2800.00",
                                    55,
                                    "https://images.unsplash.com/photo-1518977676601-b53f82aba655?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101001005",
                                    "Cebolla cabezona",
                                    "Fruver del Campo",
                                    "Cebolla blanca, malla de 500 g.",
                                    "2200.00",
                                    40,
                                    "https://images.unsplash.com/photo-1508747703725-719777637510?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101001006",
                                    "Zanahoria",
                                    "Fruver del Campo",
                                    "Zanahoria fresca, bolsa de 500 g.",
                                    "2100.00",
                                    42,
                                    "https://images.unsplash.com/photo-1598170845058-32b9d6a5da37?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101001007",
                                    "Limón Tahití",
                                    "Fruver del Campo",
                                    "Limón Tahití, bolsa de 500 g.",
                                    "2500.00",
                                    33,
                                    "https://images.unsplash.com/photo-1590502593747-42a996133562?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101001008",
                                    "Manzana roja",
                                    "Fruver del Campo",
                                    "Manzana roja importada, unidad.",
                                    "1800.00",
                                    50,
                                    "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101001009",
                                    "Naranja Valencia",
                                    "Fruver del Campo",
                                    "Naranja jugosa, bolsa de 1 kg.",
                                    "4200.00",
                                    30,
                                    "https://images.unsplash.com/photo-1547514701-42782101795e?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101001010",
                                    "Lechuga Batavia",
                                    "Fruver del Campo",
                                    "Lechuga Batavia fresca, unidad.",
                                    "2700.00",
                                    24,
                                    "https://images.unsplash.com/photo-1622206151226-18ca2c9ab4a1?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101001011",
                                    "Cilantro fresco",
                                    "Fruver del Campo",
                                    "Manojo de cilantro fresco.",
                                    "1200.00",
                                    60,
                                    "https://images.unsplash.com/photo-1584270354949-c26b0d5b4a0c?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101001012",
                                    "Plátano verde",
                                    "Fruver del Campo",
                                    "Plátano verde para patacones, unidad.",
                                    "1500.00",
                                    45,
                                    "https://images.unsplash.com/photo-1603833665858-e61d17a86224?auto=format&fit=crop&w=800&q=80"))),
            new CategorySeed(
                    "lacteos",
                    "Lácteos",
                    "Leche, yogurt, quesos y derivados.",
                    List.of(
                            product(
                                    "7702101002001",
                                    "Leche entera 1 L",
                                    "Colanta",
                                    "Leche entera pasteurizada en bolsa de un litro.",
                                    "4200.00",
                                    70,
                                    "https://images.unsplash.com/photo-1563636619-e9143da7973b?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101002002",
                                    "Leche deslactosada 1 L",
                                    "Alquería",
                                    "Leche deslactosada UHT en caja.",
                                    "4800.00",
                                    45,
                                    "https://images.unsplash.com/photo-1550583724-b2692b85b150?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101002003",
                                    "Yogurt natural 1 L",
                                    "Alpina",
                                    "Yogurt natural cremoso sin azúcar añadida.",
                                    "7800.00",
                                    32,
                                    "https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101002004",
                                    "Yogurt griego 150 g",
                                    "Alpina",
                                    "Yogurt griego natural individual.",
                                    "3200.00",
                                    40,
                                    "https://images.unsplash.com/photo-1571212515416-fef01fc43637?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101002005",
                                    "Queso campesino 500 g",
                                    "Colanta",
                                    "Queso fresco campesino en bloque.",
                                    "9800.00",
                                    22,
                                    "https://images.unsplash.com/photo-1486297678162-eb2a19b0a32d?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101002006",
                                    "Queso mozzarella 250 g",
                                    "Alpina",
                                    "Mozzarella rallada para pizza y pastas.",
                                    "8900.00",
                                    26,
                                    "https://images.unsplash.com/photo-1618164435735-413d3bb0cd63?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101002007",
                                    "Mantequilla 250 g",
                                    "La Lechera",
                                    "Mantequilla con sal en barra.",
                                    "7500.00",
                                    28,
                                    "https://images.unsplash.com/photo-1589985270826-4b7bb135bc9d?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101002008",
                                    "Crema de leche 200 ml",
                                    "Alpina",
                                    "Crema de leche para cocinar.",
                                    "4100.00",
                                    35,
                                    "https://images.unsplash.com/photo-1628088062853-cc23850194ed?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101002009",
                                    "Kumis 1 L",
                                    "Alpina",
                                    "Kumis tradicional colombiano.",
                                    "6900.00",
                                    20,
                                    "https://images.unsplash.com/photo-1623065425238-b8d1f4d9f2e0?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101002010",
                                    "Avena liquida 1 L",
                                    "Alpina",
                                    "Avena lista para beber sabor natural.",
                                    "5600.00",
                                    30,
                                    "https://images.unsplash.com/photo-1606313564200-e75d5e30476c?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101002011",
                                    "Huevos AA x 30",
                                    "Avinal",
                                    "Huevos frescos categoría AA, cubeta de 30.",
                                    "18500.00",
                                    18,
                                    "https://images.unsplash.com/photo-1582722872445-44dc5f7e3c8f?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101002012",
                                    "Queso crema 230 g",
                                    "Philadelphia",
                                    "Queso crema untablo para panadería.",
                                    "9200.00",
                                    0,
                                    "https://images.unsplash.com/photo-1452195100486-9cc805987862?auto=format&fit=crop&w=800&q=80"))),
            new CategorySeed(
                    "carnes",
                    "Carnes",
                    "Res, pollo, cerdo y embutidos.",
                    List.of(
                            product(
                                    "7702101003001",
                                    "Pechuga de pollo 500 g",
                                    "Friko",
                                    "Pechuga de pollo deshuesada en bandeja.",
                                    "12500.00",
                                    24,
                                    "https://images.unsplash.com/photo-1604503468506-a8da13d82791?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101003002",
                                    "Muslos de pollo 1 kg",
                                    "Friko",
                                    "Muslos de pollo con piel, bandeja familiar.",
                                    "9800.00",
                                    20,
                                    "https://images.unsplash.com/photo-1587593810167-a84920ea13a4?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101003003",
                                    "Carne molida 500 g",
                                    "Zenú",
                                    "Carne de res molida magra.",
                                    "14200.00",
                                    16,
                                    "https://images.unsplash.com/photo-1603048297172-c92544797d0a?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101003004",
                                    "Sobrebarriga 1 kg",
                                    "Carnes Premium",
                                    "Sobrebarriga de res para sudado.",
                                    "18900.00",
                                    12,
                                    "https://images.unsplash.com/photo-1551028150-64b9f398f678?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101003005",
                                    "Costilla de cerdo 1 kg",
                                    "Carnes Premium",
                                    "Costilla de cerdo fresca.",
                                    "16500.00",
                                    14,
                                    "https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101003006",
                                    "Chorizo antioqueño x 4",
                                    "Zenú",
                                    "Chorizo antioqueño paquete de 4 unidades.",
                                    "8900.00",
                                    30,
                                    "https://images.unsplash.com/photo-1607623814075-e51df1bdc82f?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101003007",
                                    "Salchicha ranchera x 10",
                                    "Zenú",
                                    "Salchicha ranchera paquete familiar.",
                                    "11200.00",
                                    28,
                                    "https://images.unsplash.com/photo-1615937657715-bc7b4b7962c1?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101003008",
                                    "Jamón de cerdo 250 g",
                                    "Ranchera",
                                    "Jamón cocido en lonchas.",
                                    "7800.00",
                                    22,
                                    "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101003009",
                                    "Tocino ahumado 200 g",
                                    "Ranchera",
                                    "Tocino ahumado en tiras.",
                                    "9500.00",
                                    18,
                                    "https://images.unsplash.com/photo-1606851093855-5049ca345114?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101003010",
                                    "Hígado de pollo 500 g",
                                    "Friko",
                                    "Hígado de pollo fresco.",
                                    "6200.00",
                                    15,
                                    "https://images.unsplash.com/photo-1604503468506-a8da13d82791?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101003011",
                                    "Filete de tilapia 400 g",
                                    "Pescados del Pacífico",
                                    "Filete de tilapia congelado.",
                                    "13800.00",
                                    10,
                                    "https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101003012",
                                    "Atún en aceite 160 g",
                                    "Van Camp's",
                                    "Lata de atún en aceite.",
                                    "6900.00",
                                    40,
                                    "https://images.unsplash.com/photo-1599083038492-0c8b4c8c5d0a?auto=format&fit=crop&w=800&q=80"))),
            new CategorySeed(
                    "bebidas",
                    "Bebidas",
                    "Aguas, jugos, gaseosas y café.",
                    List.of(
                            product(
                                    "7702101004001",
                                    "Agua sin gas 600 ml",
                                    "Cristal",
                                    "Botella de agua sin gas lista para llevar.",
                                    "1800.00",
                                    90,
                                    "https://images.unsplash.com/photo-1548839140-46a3dd4b1b0c?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101004002",
                                    "Agua con gas 300 ml",
                                    "Prestige",
                                    "Agua mineral con gas.",
                                    "2200.00",
                                    48,
                                    "https://images.unsplash.com/photo-1560026301-88338bae191e?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101004003",
                                    "Gaseosa cola 1.5 L",
                                    "Coca-Cola",
                                    "Gaseosa cola botella familiar.",
                                    "5500.00",
                                    55,
                                    "https://images.unsplash.com/photo-1554866585-cd94860890b7?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101004004",
                                    "Gaseosa limón 1.5 L",
                                    "Sprite",
                                    "Gaseosa limón botella familiar.",
                                    "5200.00",
                                    40,
                                    "https://images.unsplash.com/photo-1625772299848-391b6a87d7b3?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101004005",
                                    "Jugo Hit mango 1 L",
                                    "Hit",
                                    "Jugo de mango listo para beber.",
                                    "4800.00",
                                    35,
                                    "https://images.unsplash.com/photo-1600271886742-f049cd451bba?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101004006",
                                    "Jugo Hit naranja 1 L",
                                    "Hit",
                                    "Jugo de naranja listo para beber.",
                                    "4800.00",
                                    35,
                                    "https://images.unsplash.com/photo-1621506289937-a8e4df240d0b?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101004007",
                                    "Café molido 500 g",
                                    "Juan Valdez",
                                    "Café molido de origen colombiano.",
                                    "18900.00",
                                    25,
                                    "https://images.unsplash.com/photo-1447933601403-0c6688de566e?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101004008",
                                    "Café instantáneo 170 g",
                                    "Nescafé",
                                    "Café soluble clásico.",
                                    "14500.00",
                                    30,
                                    "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101004009",
                                    "Té verde x 20",
                                    "Hindú",
                                    "Caja de té verde, 20 sobres.",
                                    "6900.00",
                                    28,
                                    "https://images.unsplash.com/photo-1564890369478-c89ca6d9cde9?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101004010",
                                    "Cerveza lager x 6",
                                    "Águila",
                                    "Sixpack de cerveza lager 330 ml.",
                                    "18900.00",
                                    20,
                                    "https://images.unsplash.com/photo-1608270586620-248524c67de9?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101004011",
                                    "Bebida energizante 250 ml",
                                    "Red Bull",
                                    "Lata de bebida energizante.",
                                    "7500.00",
                                    32,
                                    "https://images.unsplash.com/photo-1622543925917-763c34d1a486?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101004012",
                                    "Chocolate de mesa 500 g",
                                    "Luker",
                                    "Chocolate de mesa para preparar.",
                                    "9800.00",
                                    22,
                                    "https://images.unsplash.com/photo-1511381939415-e44015466834?auto=format&fit=crop&w=800&q=80"))),
            new CategorySeed(
                    "despensa",
                    "Despensa",
                    "Arroz, pasta, aceites, granos y enlatados.",
                    List.of(
                            product(
                                    "7702101005001",
                                    "Arroz blanco 1 kg",
                                    "Diana",
                                    "Arroz de grano largo para el día a día.",
                                    "5200.00",
                                    60,
                                    "https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101005002",
                                    "Arroz integral 1 kg",
                                    "Diana",
                                    "Arroz integral de grano largo.",
                                    "6100.00",
                                    28,
                                    "https://images.unsplash.com/photo-1536304993881-ff6e9eefa2a6?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101005003",
                                    "Pasta spaghetti 500 g",
                                    "Doria",
                                    "Pasta spaghetti seca.",
                                    "3900.00",
                                    45,
                                    "https://images.unsplash.com/photo-1551462147-ff290249bf7f?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101005004",
                                    "Aceite vegetal 1 L",
                                    "Team",
                                    "Aceite vegetal para freír y cocinar.",
                                    "9800.00",
                                    35,
                                    "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101005005",
                                    "Aceite de oliva 500 ml",
                                    "Olivetto",
                                    "Aceite de oliva extra virgen.",
                                    "18900.00",
                                    18,
                                    "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101005006",
                                    "Frijol cargamanto 500 g",
                                    "Diana",
                                    "Frijol cargamanto seco.",
                                    "5600.00",
                                    30,
                                    "https://images.unsplash.com/photo-1596797038530-2c107229654b?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101005007",
                                    "Lenteja 500 g",
                                    "Diana",
                                    "Lenteja seca para guisos.",
                                    "4200.00",
                                    32,
                                    "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101005008",
                                    "Azúcar blanca 1 kg",
                                    "Incauca",
                                    "Azúcar refinada blanca.",
                                    "4800.00",
                                    50,
                                    "https://images.unsplash.com/photo-1587049352846-4a222e784d38?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101005009",
                                    "Sal refinada 1 kg",
                                    "Refisal",
                                    "Sal de mesa yodada.",
                                    "1800.00",
                                    55,
                                    "https://images.unsplash.com/photo-1518110925495-5fe2b5d0b0b5?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101005010",
                                    "Harina de trigo 1 kg",
                                    "Haz de Oros",
                                    "Harina de trigo para panadería.",
                                    "4500.00",
                                    38,
                                    "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101005011",
                                    "Salsa de tomate 200 g",
                                    "Fruco",
                                    "Salsa de tomate clásica.",
                                    "3500.00",
                                    42,
                                    "https://images.unsplash.com/photo-1472476443507-c7a9bc58ca5c?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101005012",
                                    "Mayonesa 400 g",
                                    "Fruco",
                                    "Mayonesa cremosa en frasco.",
                                    "8900.00",
                                    26,
                                    "https://images.unsplash.com/photo-1585598552665-8f8b8b0b0b0b?auto=format&fit=crop&w=800&q=80"))),
            new CategorySeed(
                    "aseo",
                    "Aseo del hogar",
                    "Limpieza, papelería y cuidado del hogar.",
                    List.of(
                            product(
                                    "7702101007001",
                                    "Detergente líquido 1 L",
                                    "Fab",
                                    "Detergente líquido para ropa de color.",
                                    "14500.00",
                                    28,
                                    "https://images.unsplash.com/photo-1585421514738-01798e348b17?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101007002",
                                    "Detergente en polvo 1 kg",
                                    "Ariel",
                                    "Detergente en polvo multiusos.",
                                    "12800.00",
                                    24,
                                    "https://images.unsplash.com/photo-1610557892470-55d9e80c0bce?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101007003",
                                    "Suavizante 1 L",
                                    "Downy",
                                    "Suavizante de ropa aroma floral.",
                                    "15900.00",
                                    20,
                                    "https://images.unsplash.com/photo-1610557892470-55d9e80c0bce?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101007004",
                                    "Jabón en barra x 3",
                                    "Rey",
                                    "Jabón de lavar en barra, paquete x 3.",
                                    "6900.00",
                                    35,
                                    "https://images.unsplash.com/photo-1585421514738-01798e348b17?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101007005",
                                    "Lavaloza 500 ml",
                                    "Axion",
                                    "Lavaloza líquido concentrado.",
                                    "7800.00",
                                    30,
                                    "https://images.unsplash.com/photo-1563453392212-326f5e854473?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101007006",
                                    "Limpiavidrios 500 ml",
                                    "Easy-Off",
                                    "Limpiavidrios con atomizador.",
                                    "9200.00",
                                    22,
                                    "https://images.unsplash.com/photo-1581578731548-c64695cc6952?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101007007",
                                    "Cloro 1 L",
                                    "Blancox",
                                    "Blanqueador clorado para desinfección.",
                                    "4500.00",
                                    40,
                                    "https://images.unsplash.com/photo-1581578731548-c64695cc6952?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101007008",
                                    "Papel higiénico x 12",
                                    "Familia",
                                    "Papel higiénico doble hoja, paquete x 12.",
                                    "18900.00",
                                    26,
                                    "https://images.unsplash.com/photo-1584556812952-905ffd0c611a?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101007009",
                                    "Servilletas x 100",
                                    "Familia",
                                    "Servilletas de papel, paquete x 100.",
                                    "4200.00",
                                    38,
                                    "https://images.unsplash.com/photo-1584556812952-905ffd0c611a?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101007010",
                                    "Esponja multiusos x 3",
                                    "Scotch-Brite",
                                    "Esponjas de cocina, paquete x 3.",
                                    "5900.00",
                                    34,
                                    "https://images.unsplash.com/photo-1563453392212-326f5e854473?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101007011",
                                    "Bolsas de basura x 20",
                                    "Glad",
                                    "Bolsas de basura medianas, rollo x 20.",
                                    "8900.00",
                                    28,
                                    "https://images.unsplash.com/photo-1610557892470-55d9e80c0bce?auto=format&fit=crop&w=800&q=80"),
                            product(
                                    "7702101007012",
                                    "Ambientador spray 300 ml",
                                    "Glade",
                                    "Ambientador aerosol aroma limón.",
                                    "11200.00",
                                    18,
                                    "https://images.unsplash.com/photo-1581578731548-c64695cc6952?auto=format&fit=crop&w=800&q=80"))));

    private final ProductRepository productRepository;
    private final ProductTypeRepository productTypeRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final CreateCategoryUseCase createCategoryUseCase;
    private final CreateProductUseCase createProductUseCase;
    private final CreateDocumentUseCase createDocumentUseCase;
    private final ListDocumentsUseCase listDocumentsUseCase;
    private final ProcessDocumentUseCase processDocumentUseCase;
    private final Clock clock;
    private final boolean enabled;

    public LocalDemoDataRunner(
            ProductRepository productRepository,
            ProductTypeRepository productTypeRepository,
            KnowledgeDocumentRepository documentRepository,
            CreateCategoryUseCase createCategoryUseCase,
            CreateProductUseCase createProductUseCase,
            CreateDocumentUseCase createDocumentUseCase,
            ListDocumentsUseCase listDocumentsUseCase,
            ProcessDocumentUseCase processDocumentUseCase,
            Clock clock,
            @Value("${superfercho.dev.demo-seed.enabled:true}") boolean enabled) {
        this.productRepository = productRepository;
        this.productTypeRepository = productTypeRepository;
        this.documentRepository = documentRepository;
        this.createCategoryUseCase = createCategoryUseCase;
        this.createProductUseCase = createProductUseCase;
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
        if (!productRepository.findAll().isEmpty()) {
            LOGGER.info("Local base catalog already present; skipping catalog seed");
            return;
        }

        Map<String, UUID> categoryIds = new LinkedHashMap<>();
        int productCount = 0;
        for (CategorySeed category : CATEGORIES) {
            UUID categoryId = createCategory(category.name(), category.description());
            categoryIds.put(category.key(), categoryId);
            UUID productTypeId = createProductType(categoryId, category.name());
            for (ProductSeed product : category.products()) {
                createProduct(productTypeId, product);
                productCount++;
            }
        }

        LOGGER.info(
                "Local base catalog seeded ({} categories, {} products).",
                categoryIds.size(),
                productCount);
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

    private ProductResult createProduct(UUID productTypeId, ProductSeed product) {
        return createProductUseCase.execute(new CreateProductCommand(
                productTypeId,
                null,
                Presentation.of(1, PresentationUnit.UNIT),
                product.barcode(),
                product.name(),
                product.brand(),
                product.description(),
                Money.cop(new BigDecimal(product.price())),
                product.stock(),
                product.imageUrl()));
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

    private static ProductSeed product(
            String barcode,
            String name,
            String brand,
            String description,
            String price,
            int stock,
            String imageUrl) {
        return new ProductSeed(barcode, name, brand, description, price, stock, imageUrl);
    }

    private record CategorySeed(String key, String name, String description, List<ProductSeed> products) {
    }

    private record ProductSeed(
            String barcode,
            String name,
            String brand,
            String description,
            String price,
            int stock,
            String imageUrl) {
    }
}

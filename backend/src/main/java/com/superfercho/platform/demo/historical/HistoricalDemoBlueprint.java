package com.superfercho.platform.demo.historical;

import java.util.List;

/**
 * Approved historical demo scenario (C01–C10). Data only — no persistence.
 */
public final class HistoricalDemoBlueprint {

    public static final String DOCUMENT_TYPE = "CC";
    public static final String PASSWORD = "SuperF123!";
    public static final String CITY = "Cúcuta";
    public static final String DEPARTMENT = "Norte de Santander";
    public static final int EXPECTED_CUSTOMERS = 10;
    public static final int EXPECTED_ORDERS = 32;

    private HistoricalDemoBlueprint() {}

    public static List<CustomerSpec> customers() {
        return List.of(
                new CustomerSpec(
                        "C01",
                        "Ana María",
                        "Rodríguez Gómez",
                        "1018473625",
                        "ana.rodriguez@gmail.com",
                        "3157284061",
                        List.of(
                                address("Casa", "Calle 12 # 5-48", "Barrio La Riviera", "3157284061", true),
                                address("Oficina", "Avenida 0 # 11-24", "Oficina 504", "3157284061", false)),
                        List.of("SF-001", "SF-018", "SF-006", "SF-027", "SF-046", "SF-053", "SF-087", "SF-141"),
                        List.of(
                                list("Mercado semanal", "SF-001", "SF-018", "SF-027", "SF-035", "SF-046", "SF-081", "SF-089", "SF-090"),
                                list("Desayunos", "SF-001", "SF-018", "SF-006", "SF-013", "SF-051")),
                        List.of(),
                        List.of(
                                order(OrderOutcome.DELIVERED, "SF-001", "SF-018", "SF-027"),
                                order(OrderOutcome.DELIVERED, "SF-006", "SF-046", "SF-053"),
                                order(OrderOutcome.DELIVERED, "SF-035", "SF-081", "SF-089", "SF-090"),
                                order(OrderOutcome.DELIVERED, "SF-013", "SF-051", "SF-087", "SF-141"),
                                order(OrderOutcome.CANCELLED, "SF-001", "SF-018"))),
                new CustomerSpec(
                        "C02",
                        "Carlos Andrés",
                        "Martínez Pérez",
                        "1036291748",
                        "carlos.martinez@gmail.com",
                        "3016948275",
                        List.of(address("Casa", "Calle 8 # 13-72", "Casa esquinera", "3016948275", true)),
                        List.of("SF-030", "SF-036", "SF-049", "SF-056", "SF-074"),
                        List.of(list(
                                "Mercado familiar",
                                "SF-030",
                                "SF-036",
                                "SF-035",
                                "SF-046",
                                "SF-031",
                                "SF-039",
                                "SF-089",
                                "SF-090",
                                "SF-003",
                                "SF-018")),
                        List.of("SF-003", "SF-031", "SF-053", "SF-086"),
                        List.of(
                                order(OrderOutcome.DELIVERED, "SF-030", "SF-036", "SF-035", "SF-046"),
                                order(OrderOutcome.DELIVERED, "SF-031", "SF-039", "SF-003", "SF-018"),
                                order(OrderOutcome.DELIVERED, "SF-049", "SF-056", "SF-074", "SF-089"),
                                order(OrderOutcome.CANCELLED, "SF-090", "SF-036"))),
                new CustomerSpec(
                        "C03",
                        "Laura Sofía",
                        "Hernández Rojas",
                        "1013958274",
                        "laura.hernandez@gmail.com",
                        "3184072956",
                        List.of(
                                address(
                                        "Edificio Los Robles",
                                        "Avenida 6 # 15-36",
                                        "Torre 2, apartamento 201",
                                        "3184072956",
                                        true),
                                address("Casa de mamá", "Calle 18 # 9-41", "Casa 3", "3184072956", false)),
                        List.of("SF-002", "SF-007", "SF-018", "SF-029", "SF-086", "SF-131", "SF-142"),
                        List.of(list(
                                "Compras de la semana",
                                "SF-002",
                                "SF-007",
                                "SF-029",
                                "SF-081",
                                "SF-086",
                                "SF-133",
                                "SF-141")),
                        List.of(),
                        List.of(
                                order(OrderOutcome.DELIVERED, "SF-002", "SF-007", "SF-018"),
                                order(OrderOutcome.DELIVERED, "SF-029", "SF-081", "SF-086"),
                                order(OrderOutcome.DELIVERED, "SF-131", "SF-133", "SF-141", "SF-142"),
                                order(OrderOutcome.CANCELLED, "SF-002", "SF-029"))),
                new CustomerSpec(
                        "C04",
                        "Andrés Felipe",
                        "Gómez Torres",
                        "1027483916",
                        "andres.gomez@gmail.com",
                        "3205817439",
                        List.of(address(
                                "Edificio Mirador", "Calle 11 # 7-58", "Apartamento 604", "3205817439", true)),
                        List.of("SF-022", "SF-024", "SF-057"),
                        List.of(list("Parrillada", "SF-022", "SF-024", "SF-025", "SF-026", "SF-053", "SF-091")),
                        List.of("SF-009", "SF-057"),
                        List.of(
                                order(OrderOutcome.DELIVERED, "SF-022", "SF-024", "SF-025", "SF-026"),
                                order(OrderOutcome.DELIVERED, "SF-053", "SF-091", "SF-057"),
                                order(OrderOutcome.CANCELLED, "SF-022", "SF-024"))),
                new CustomerSpec(
                        "C05",
                        "Valentina",
                        "Sánchez Moreno",
                        "1049362715",
                        "valentina.sanchez@gmail.com",
                        "3138462057",
                        List.of(address(
                                "Urbanización Los Almendros",
                                "Calle 20 # 4-19",
                                "Casa 16",
                                "3138462057",
                                true)),
                        List.of("SF-006", "SF-085", "SF-133", "SF-141"),
                        List.of(list(
                                "Casa",
                                "SF-105",
                                "SF-109",
                                "SF-112",
                                "SF-116",
                                "SF-118",
                                "SF-125",
                                "SF-127",
                                "SF-129")),
                        List.of("SF-081", "SF-085", "SF-105", "SF-112", "SF-141"),
                        List.of(
                                order(OrderOutcome.DELIVERED, "SF-105", "SF-109", "SF-112", "SF-116"),
                                order(OrderOutcome.DELIVERED, "SF-118", "SF-125", "SF-127", "SF-129"),
                                order(OrderOutcome.CANCELLED, "SF-006", "SF-085", "SF-133"))),
                new CustomerSpec(
                        "C06",
                        "Sebastián",
                        "Ramírez Castro",
                        "1015826493",
                        "sebastian.ramirez@gmail.com",
                        "3162958174",
                        List.of(address(
                                "Edificio San José",
                                "Avenida 10 # 6-83",
                                "Apartamento 402",
                                "3162958174",
                                true)),
                        List.of(),
                        List.of(),
                        List.of("SF-001"),
                        List.of(order(OrderOutcome.DELIVERED, "SF-001", "SF-018", "SF-046"))),
                new CustomerSpec(
                        "C07",
                        "Daniela",
                        "Vargas Peña",
                        "1038174652",
                        "daniela.vargas@gmail.com",
                        "3007639418",
                        List.of(
                                address(
                                        "Edificio Torres del Norte",
                                        "Calle 14 # 10-27",
                                        "Torre 1, apartamento 305",
                                        "3007639418",
                                        true),
                                address("Oficina", "Calle 9 # 6-15", "Local 8", "3007639418", false)),
                        List.of(
                                "SF-003",
                                "SF-019",
                                "SF-031",
                                "SF-035",
                                "SF-046",
                                "SF-053",
                                "SF-058",
                                "SF-087",
                                "SF-105",
                                "SF-141"),
                        List.of(
                                list(
                                        "Cumpleaños",
                                        "SF-053",
                                        "SF-056",
                                        "SF-058",
                                        "SF-059",
                                        "SF-074",
                                        "SF-075",
                                        "SF-081",
                                        "SF-085",
                                        "SF-088"),
                                list("Asado", "SF-024", "SF-025", "SF-026", "SF-040", "SF-089", "SF-091", "SF-057"),
                                list(
                                        "Despensa",
                                        "SF-027",
                                        "SF-031",
                                        "SF-035",
                                        "SF-036",
                                        "SF-037",
                                        "SF-039",
                                        "SF-046",
                                        "SF-049",
                                        "SF-050",
                                        "SF-051",
                                        "SF-058",
                                        "SF-062")),
                        List.of(),
                        List.of(
                                order(OrderOutcome.DELIVERED, "SF-053", "SF-056", "SF-058", "SF-074"),
                                order(OrderOutcome.DELIVERED, "SF-024", "SF-025", "SF-026", "SF-040", "SF-091"),
                                order(OrderOutcome.CANCELLED, "SF-027", "SF-031", "SF-035"))),
                new CustomerSpec(
                        "C08",
                        "Mateo",
                        "Castillo Díaz",
                        "1024617389",
                        "mateo.castillo@gmail.com",
                        "3129586047",
                        List.of(address(
                                "Urbanización Villa Norte",
                                "Avenida 4 # 17-63",
                                "Casa 12",
                                "3129586047",
                                true)),
                        List.of("SF-027", "SF-036", "SF-040", "SF-053", "SF-068", "SF-152"),
                        List.of(list(
                                "Mercado grande",
                                "SF-003",
                                "SF-018",
                                "SF-027",
                                "SF-030",
                                "SF-035",
                                "SF-036",
                                "SF-039",
                                "SF-046",
                                "SF-049",
                                "SF-053",
                                "SF-062",
                                "SF-081",
                                "SF-089",
                                "SF-090",
                                "SF-105")),
                        List.of("SF-018", "SF-027", "SF-036", "SF-046", "SF-089", "SF-090"),
                        List.of(
                                order(OrderOutcome.DELIVERED, "SF-003", "SF-018", "SF-027", "SF-030"),
                                order(OrderOutcome.DELIVERED, "SF-035", "SF-036", "SF-039", "SF-046"),
                                order(OrderOutcome.DELIVERED, "SF-049", "SF-053", "SF-062", "SF-081"),
                                order(OrderOutcome.DELIVERED, "SF-089", "SF-090", "SF-105", "SF-040"),
                                order(OrderOutcome.CANCELLED, "SF-068", "SF-152"))),
                new CustomerSpec(
                        "C09",
                        "Camila",
                        "Torres Martínez",
                        "1019364857",
                        "camila.torres@gmail.com",
                        "3176248395",
                        List.of(address(
                                "Edificio Palma Real",
                                "Calle 16 # 8-52",
                                "Torre B, apartamento 703",
                                "3176248395",
                                true)),
                        List.of("SF-007", "SF-086"),
                        List.of(),
                        List.of(),
                        List.of(order(OrderOutcome.CANCELLED, "SF-007", "SF-086"))),
                new CustomerSpec(
                        "C10",
                        "Felipe",
                        "Moreno Silva",
                        "1042758193",
                        "felipe.moreno@gmail.com",
                        "3104872659",
                        List.of(address(
                                "Urbanización La Floresta",
                                "Calle 23 # 11-46",
                                "Casa 8",
                                "3104872659",
                                true)),
                        List.of("SF-029", "SF-031", "SF-050", "SF-062", "SF-131"),
                        List.of(list("Mercado", "SF-029", "SF-031", "SF-050", "SF-062", "SF-081", "SF-131")),
                        List.of("SF-029", "SF-062", "SF-131"),
                        List.of(
                                order(OrderOutcome.DELIVERED, "SF-029", "SF-031", "SF-050"),
                                order(OrderOutcome.DELIVERED, "SF-062", "SF-081", "SF-131"),
                                order(OrderOutcome.CANCELLED, "SF-029", "SF-050"))));
    }

    private static AddressSpec address(
            String label, String addressLine, String additionalInfo, String phone, boolean isDefault) {
        return new AddressSpec(label, addressLine, additionalInfo, CITY, DEPARTMENT, phone, isDefault);
    }

    private static ShoppingListSpec list(String name, String... productCodes) {
        return new ShoppingListSpec(name, List.of(productCodes));
    }

    private static OrderSpec order(OrderOutcome outcome, String... productCodes) {
        return new OrderSpec(outcome, List.of(productCodes));
    }

    public enum OrderOutcome {
        DELIVERED,
        CANCELLED
    }

    public record AddressSpec(
            String label,
            String addressLine,
            String additionalInfo,
            String city,
            String department,
            String phone,
            boolean isDefault) {}

    public record ShoppingListSpec(String name, List<String> productCodes) {}

    public record OrderSpec(OrderOutcome outcome, List<String> productCodes) {}

    public record CustomerSpec(
            String code,
            String firstName,
            String lastName,
            String documentNumber,
            String email,
            String phone,
            List<AddressSpec> addresses,
            List<String> favoriteProductCodes,
            List<ShoppingListSpec> shoppingLists,
            List<String> cartProductCodes,
            List<OrderSpec> orders) {

        public String fullName() {
            return firstName + " " + lastName;
        }
    }
}

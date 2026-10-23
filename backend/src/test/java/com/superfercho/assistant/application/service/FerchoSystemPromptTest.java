package com.superfercho.assistant.application.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class FerchoSystemPromptTest {

    private static String normalized() {
        return FerchoSystemPrompt.TEXT.toLowerCase(Locale.ROOT).replace("\n", " ");
    }

    @Test
    void shouldKeepPreferenceForSearchProductsOverListProducts() {
        String normalized = normalized();

        assertTrue(normalized.contains("search_products"), "el prompt debe mencionar search_products");
        assertTrue(normalized.contains("list_products"), "el prompt debe mencionar list_products");
        assertTrue(
                normalized.contains("prefiere search_products"),
                "el prompt debe establecer la preferencia por search_products");
        assertTrue(
                normalized.contains("list_products solo"),
                "el prompt debe restringir list_products al caso en que el usuario lo pide");
    }

    @Test
    void shouldKeepReferenceResolutionRule() {
        String normalized = normalized();

        assertTrue(
                normalized.contains("resuelve primero esa referencia"),
                "el prompt debe ordenar resolver referencias humanas con herramientas de consulta");
        assertTrue(
                normalized.contains("no le pidas al usuario un uuid"),
                "el prompt debe prohibir pedir al usuario identificadores técnicos obtenibles por herramientas");
    }

    @Test
    void shouldKeepSensitiveToolSequencingRule() {
        String normalized = normalized();

        assertTrue(
                normalized.contains("invoca la herramienta correspondiente"),
                "el prompt debe ordenar invocar la herramienta sensible cuando los datos estén disponibles");
        assertTrue(
                normalized.contains("solo preparan la operaci"),
                "el prompt debe aclarar que las herramientas sensibles solo preparan la operación");
        assertTrue(
                normalized.contains("el sistema solicita después la confirmación"),
                "el prompt debe mantener la confirmación bajo control del sistema");
    }

    @Test
    void shouldKeepSuperFerchoExclusiveScope() {
        String normalized = normalized();

        assertTrue(
                normalized.contains("exclusivamente a compras y servicios de superfercho"),
                "el prompt debe limitar a Fercho a SuperFercho");
        assertTrue(
                normalized.contains("elegir ingredientes"),
                "el prompt debe permitir ayuda con ingredientes");
        assertTrue(
                normalized.contains("estimar cantidades"),
                "el prompt debe permitir estimar cantidades para personas");
        assertTrue(
                normalized.contains("convertir unidades"),
                "el prompt debe permitir conversiones de unidades");
        assertTrue(
                normalized.contains("resolve_quantity_equivalence"),
                "el prompt debe anclar las conversiones a resolve_quantity_equivalence");
        assertTrue(
                normalized.contains("no ofrezcas recetas completas"),
                "el prompt debe prohibir recetas completas");
        assertTrue(
                normalized.contains("instrucciones paso a paso de preparación"),
                "el prompt debe prohibir pasos de preparación");
        assertTrue(
                normalized.contains("técnicas culinarias"),
                "el prompt debe prohibir técnicas culinarias");
        assertTrue(
                normalized.contains("tiempos de cocción"),
                "el prompt debe prohibir tiempos de cocción");
        assertTrue(
                normalized.contains("redirige en una frase breve"),
                "el prompt debe redirigir preguntas ajenas al supermercado");
    }

    @Test
    void shouldRequireBackendQuantityEquivalenceInsteadOfLlmConversion() {
        String normalized = normalized();

        assertTrue(
                normalized.contains("usa solo resolve_quantity_equivalence"),
                "el prompt debe prohibir que el modelo convierta kg/g/l/ml por su cuenta");
        assertTrue(
                normalized.contains("número entero de paquetes"),
                "el prompt debe distinguir cantidad física de cantidad de paquetes");
        assertTrue(
                normalized.contains("no modifica el carrito"),
                "el prompt debe dejar claro que la consulta no muta el carrito");
        assertTrue(
                normalized.contains("aceptación explícita"),
                "el prompt debe exigir aceptación explícita antes de add_cart_item");
        assertTrue(
                normalized.contains("un «sí» genérico no es una elección"),
                "el prompt no debe tratar un sí genérico como autorización para agregar");
    }

    @Test
    void shouldRequireBackendCartTotalsInsteadOfClientSums() {
        String normalized = normalized();

        assertTrue(
                normalized.contains("currentsubtotal"),
                "el prompt debe anclar los importes del carrito a currentSubtotal");
        assertTrue(
                normalized.contains("no sumes priceataddition"),
                "el prompt debe prohibir sumar priceAtAddition");
        assertTrue(
                normalized.contains("ese estimado no es el cobro de checkout"),
                "el prompt debe distinguir el estimado del carrito del cobro de checkout");
    }

    @Test
    void shouldKeepKnowledgeOutOfRecipesPricesStockPaymentsAndOrders() {
        String normalized = normalized();

        assertTrue(
                normalized.contains("search_knowledge no es fuente de recetas"),
                "el prompt no debe presentar search_knowledge como fuente de recetas");
        assertTrue(normalized.contains("precios"), "el prompt debe seguir excluyendo precios de knowledge");
        assertTrue(normalized.contains("stock"), "el prompt debe seguir excluyendo stock de knowledge");
        assertTrue(normalized.contains("pagos"), "el prompt debe excluir pagos de knowledge");
        assertTrue(
                normalized.contains("estados de pedido"),
                "el prompt debe excluir estados de pedido de knowledge");
    }

    @Test
    void shouldKeepToolFailurePolicy() {
        String normalized = normalized();

        assertTrue(
                normalized.contains("cuando una herramienta falle o devuelva resultados insuficientes"),
                "el prompt debe definir la política ante fallo o resultados insuficientes de una tool");
        assertTrue(
                normalized.contains("sin exponer detalles"),
                "el prompt debe prohibir exponer detalles técnicos del fallo");
        assertTrue(
                normalized.contains("nunca presentes el fallo"),
                "el prompt debe prohibir presentar el fallo como operación realizada");
        assertTrue(
                normalized.contains("no rellenes la respuesta"),
                "el prompt debe prohibir rellenar la respuesta con datos no obtenidos");
        assertTrue(
                normalized.contains("vuelve a intentarlo solo si cambia"),
                "el prompt debe restringir los reintentos automáticos de la misma llamada");
        assertTrue(
                normalized.contains("si otra herramienta disponible puede resolver"),
                "el prompt debe permitir ofrecer alternativas con otras herramientas");
    }
}

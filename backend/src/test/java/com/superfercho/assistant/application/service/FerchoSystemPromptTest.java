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

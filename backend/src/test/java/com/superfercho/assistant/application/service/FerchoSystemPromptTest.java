package com.superfercho.assistant.application.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class FerchoSystemPromptTest {

    @Test
    void shouldKeepPreferenceForSearchProductsOverListProducts() {
        String normalized = FerchoSystemPrompt.TEXT.toLowerCase(Locale.ROOT).replace("\n", " ");

        assertTrue(normalized.contains("search_products"), "el prompt debe mencionar search_products");
        assertTrue(normalized.contains("list_products"), "el prompt debe mencionar list_products");
        assertTrue(
                normalized.contains("prefiere search_products"),
                "el prompt debe establecer la preferencia por search_products");
        assertTrue(
                normalized.contains("list_products solo"),
                "el prompt debe restringir list_products al caso en que el usuario lo pide");
    }
}

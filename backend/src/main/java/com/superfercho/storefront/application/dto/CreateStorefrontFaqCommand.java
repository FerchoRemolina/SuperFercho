package com.superfercho.storefront.application.dto;

/** Creación de una pregunta frecuente. */
public record CreateStorefrontFaqCommand(String question, String answer, int displayOrder, boolean active) {}

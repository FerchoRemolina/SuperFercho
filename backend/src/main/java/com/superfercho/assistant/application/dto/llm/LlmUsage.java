package com.superfercho.assistant.application.dto.llm;

/**
 * Uso de tokens reportado por el proveedor. Null cuando el proveedor no
 * entrega datos válidos: nunca se inventa.
 */
public record LlmUsage(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
}

package com.superfercho.storefront.application.dto;

import java.util.List;
import java.util.UUID;

/** Reordenación en bloque: los ids en el orden de visualización deseado. */
public record ReorderStorefrontFaqsCommand(List<UUID> orderedIds) {}

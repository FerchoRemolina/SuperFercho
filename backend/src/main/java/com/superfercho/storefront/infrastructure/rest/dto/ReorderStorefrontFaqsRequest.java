package com.superfercho.storefront.infrastructure.rest.dto;

import java.util.List;
import java.util.UUID;

public record ReorderStorefrontFaqsRequest(List<UUID> orderedIds) {}

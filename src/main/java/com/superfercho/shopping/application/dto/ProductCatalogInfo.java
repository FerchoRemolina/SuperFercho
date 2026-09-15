package com.superfercho.shopping.application.dto;

import com.superfercho.platform.money.Money;
import java.util.UUID;

public record ProductCatalogInfo(UUID productId, Money currentPrice) {}

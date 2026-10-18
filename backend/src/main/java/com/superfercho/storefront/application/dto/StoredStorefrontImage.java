package com.superfercho.storefront.application.dto;

/** Imagen almacenada por Media: key estable + content type canónico. */
public record StoredStorefrontImage(String storageKey, String contentType) {}

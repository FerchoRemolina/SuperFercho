package com.superfercho.orders.application.dto;

import java.util.UUID;

/**
 * Commercial identity of an orders.customer_id (identity.users.id), resolved
 * through the CustomerDirectoryPort. Immutable consumer-owned view: orders
 * never depend on Identity domain types.
 *
 * <p>{@code fullName} and the document come from the CustomerRecord (billing
 * identity) when the account has one; otherwise they fall back to the
 * account's own registration data. {@code email}/{@code phone} belong to the
 * account and may be null.
 */
public record CustomerDirectoryEntry(
        UUID userId,
        String fullName,
        String documentType,
        String documentNumber,
        String email,
        String phone) {}

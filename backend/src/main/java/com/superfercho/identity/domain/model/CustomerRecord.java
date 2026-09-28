package com.superfercho.identity.domain.model;

import com.superfercho.identity.domain.exception.InvalidCustomerRecordException;
import java.time.Instant;
import java.util.UUID;

public final class CustomerRecord {

    private final UUID id;
    private final String documentType;
    private final String documentNumber;
    private final String billingFirstName;
    private final String billingLastName;
    private final Instant createdAt;
    private final Instant updatedAt;

    private CustomerRecord(
            UUID id,
            String documentType,
            String documentNumber,
            String billingFirstName,
            String billingLastName,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.billingFirstName = billingFirstName;
        this.billingLastName = billingLastName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CustomerRecord create(
            UUID id,
            String documentType,
            String documentNumber,
            String billingFirstName,
            String billingLastName,
            Instant createdAt,
            Instant updatedAt) {
        requireNonNull(id, "id");
        requireText(documentType, "documentType");
        requireText(documentNumber, "documentNumber");
        requireText(billingFirstName, "billingFirstName");
        requireText(billingLastName, "billingLastName");
        requireNonNull(createdAt, "createdAt");
        requireNonNull(updatedAt, "updatedAt");
        if (createdAt.isAfter(updatedAt)) {
            throw new InvalidCustomerRecordException("createdAt must not be after updatedAt");
        }
        return new CustomerRecord(
                id,
                documentType,
                documentNumber,
                billingFirstName,
                billingLastName,
                createdAt,
                updatedAt);
    }

    public UUID id() {
        return id;
    }

    public String documentType() {
        return documentType;
    }

    public String documentNumber() {
        return documentNumber;
    }

    public String billingFirstName() {
        return billingFirstName;
    }

    public String billingLastName() {
        return billingLastName;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new InvalidCustomerRecordException(field + " cannot be null");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidCustomerRecordException(field + " cannot be null or blank");
        }
    }
}

package com.superfercho.identity.domain.model;

import com.superfercho.identity.domain.exception.InvalidAddressException;
import java.time.Instant;
import java.util.UUID;

public final class Address {

    private final UUID id;
    private final String label;
    private final String recipientName;
    private final String addressLine;
    private final String additionalInfo;
    private final String city;
    private final String department;
    private final String phone;
    private final boolean isDefault;
    private final AddressStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Address(
            UUID id,
            String label,
            String recipientName,
            String addressLine,
            String additionalInfo,
            String city,
            String department,
            String phone,
            boolean isDefault,
            AddressStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.label = label;
        this.recipientName = recipientName;
        this.addressLine = addressLine;
        this.additionalInfo = additionalInfo;
        this.city = city;
        this.department = department;
        this.phone = phone;
        this.isDefault = isDefault;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Address create(
            UUID id,
            String label,
            String recipientName,
            String addressLine,
            String additionalInfo,
            String city,
            String department,
            String phone,
            boolean isDefault,
            AddressStatus status,
            Instant createdAt,
            Instant updatedAt) {
        requireNonNull(id, "id");
        requireText(label, "label");
        requireText(recipientName, "recipientName");
        requireText(addressLine, "addressLine");
        requireText(city, "city");
        requireText(department, "department");
        requireText(phone, "phone");
        requireNonNull(status, "status");
        requireNonNull(createdAt, "createdAt");
        requireNonNull(updatedAt, "updatedAt");
        if (createdAt.isAfter(updatedAt)) {
            throw new InvalidAddressException("createdAt must not be after updatedAt");
        }
        if (status == AddressStatus.INACTIVE && isDefault) {
            throw new InvalidAddressException("inactive address cannot be a default address");
        }

        return new Address(
                id,
                label,
                recipientName,
                addressLine,
                additionalInfo,
                city,
                department,
                phone,
                isDefault,
                status,
                createdAt,
                updatedAt);
    }

    public Address deactivate(Instant updatedAt) {
        requireNonNull(updatedAt, "updatedAt");
        if (createdAt.isAfter(updatedAt)) {
            throw new InvalidAddressException("createdAt must not be after updatedAt");
        }
        return new Address(
                id,
                label,
                recipientName,
                addressLine,
                additionalInfo,
                city,
                department,
                phone,
                false,
                AddressStatus.INACTIVE,
                createdAt,
                updatedAt);
    }

    public UUID id() {
        return id;
    }

    public String label() {
        return label;
    }

    public String recipientName() {
        return recipientName;
    }

    public String addressLine() {
        return addressLine;
    }

    public String additionalInfo() {
        return additionalInfo;
    }

    public String city() {
        return city;
    }

    public String department() {
        return department;
    }

    public String phone() {
        return phone;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public AddressStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new InvalidAddressException(field + " cannot be null");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidAddressException(field + " cannot be null or blank");
        }
    }
}

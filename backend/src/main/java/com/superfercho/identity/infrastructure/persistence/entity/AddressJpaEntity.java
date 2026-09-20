package com.superfercho.identity.infrastructure.persistence.entity;

import com.superfercho.identity.domain.model.AddressStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "addresses", schema = "identity")
public class AddressJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "address_line", nullable = false)
    private String addressLine;

    @Column(name = "additional_info")
    private String additionalInfo;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "department", nullable = false)
    private String department;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AddressStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AddressJpaEntity() {
    }

    public AddressJpaEntity(
            UUID id,
            UUID userId,
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
        this.userId = userId;
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

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getLabel() {
        return label;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public String getCity() {
        return city;
    }

    public String getDepartment() {
        return department;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public AddressStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

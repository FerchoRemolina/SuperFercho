package com.superfercho.identity.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_records", schema = "identity")
public class CustomerRecordJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "document_number", nullable = false)
    private String documentNumber;

    @Column(name = "billing_first_name", nullable = false)
    private String billingFirstName;

    @Column(name = "billing_last_name", nullable = false)
    private String billingLastName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CustomerRecordJpaEntity() {}

    public CustomerRecordJpaEntity(
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

    public UUID getId() {
        return id;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public String getBillingFirstName() {
        return billingFirstName;
    }

    public String getBillingLastName() {
        return billingLastName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

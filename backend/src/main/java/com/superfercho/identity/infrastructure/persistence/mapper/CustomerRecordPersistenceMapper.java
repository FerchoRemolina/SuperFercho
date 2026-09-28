package com.superfercho.identity.infrastructure.persistence.mapper;

import com.superfercho.identity.domain.model.CustomerRecord;
import com.superfercho.identity.infrastructure.persistence.entity.CustomerRecordJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CustomerRecordPersistenceMapper {

    public CustomerRecordJpaEntity toEntity(CustomerRecord customerRecord) {
        return new CustomerRecordJpaEntity(
                customerRecord.id(),
                customerRecord.documentType(),
                customerRecord.documentNumber(),
                customerRecord.billingFirstName(),
                customerRecord.billingLastName(),
                customerRecord.createdAt(),
                customerRecord.updatedAt());
    }

    public CustomerRecord toDomain(CustomerRecordJpaEntity entity) {
        return CustomerRecord.create(
                entity.getId(),
                entity.getDocumentType(),
                entity.getDocumentNumber(),
                entity.getBillingFirstName(),
                entity.getBillingLastName(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}

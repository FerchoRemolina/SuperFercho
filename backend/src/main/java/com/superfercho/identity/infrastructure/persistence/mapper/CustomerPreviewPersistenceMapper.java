package com.superfercho.identity.infrastructure.persistence.mapper;

import com.superfercho.identity.domain.model.CustomerPreview;
import com.superfercho.identity.infrastructure.persistence.entity.CustomerPreviewJpaEntity;

public final class CustomerPreviewPersistenceMapper {

    private CustomerPreviewPersistenceMapper() {}

    public static CustomerPreviewJpaEntity toEntity(CustomerPreview preview) {
        return new CustomerPreviewJpaEntity(
                preview.id(),
                preview.adminUserId(),
                preview.temporaryCustomerId(),
                preview.status(),
                preview.createdAt(),
                preview.expiresAt(),
                preview.closedAt());
    }

    public static CustomerPreview toDomain(CustomerPreviewJpaEntity entity) {
        return CustomerPreview.reconstitute(
                entity.getId(),
                entity.getAdminUserId(),
                entity.getTemporaryCustomerId(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getClosedAt());
    }
}

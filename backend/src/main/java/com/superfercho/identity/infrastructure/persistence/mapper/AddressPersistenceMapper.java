package com.superfercho.identity.infrastructure.persistence.mapper;

import com.superfercho.identity.domain.model.Address;
import com.superfercho.identity.infrastructure.persistence.entity.AddressJpaEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AddressPersistenceMapper {

    public AddressJpaEntity toEntity(UUID userId, Address address) {
        return new AddressJpaEntity(
                address.id(),
                userId,
                address.label(),
                address.recipientName(),
                address.addressLine(),
                address.additionalInfo(),
                address.city(),
                address.department(),
                address.phone(),
                address.isDefault(),
                address.status(),
                address.createdAt(),
                address.updatedAt());
    }

    public Address toDomain(AddressJpaEntity entity) {
        return Address.create(
                entity.getId(),
                entity.getLabel(),
                entity.getRecipientName(),
                entity.getAddressLine(),
                entity.getAdditionalInfo(),
                entity.getCity(),
                entity.getDepartment(),
                entity.getPhone(),
                entity.isDefault(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}

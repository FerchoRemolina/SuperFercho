package com.superfercho.identity.infrastructure.persistence;

import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.domain.model.Address;
import com.superfercho.identity.infrastructure.persistence.mapper.AddressPersistenceMapper;
import com.superfercho.identity.infrastructure.persistence.repository.AddressJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class AddressPersistenceAdapter implements AddressRepository {

    private final AddressJpaRepository addressJpaRepository;
    private final AddressPersistenceMapper addressPersistenceMapper;

    public AddressPersistenceAdapter(
            AddressJpaRepository addressJpaRepository,
            AddressPersistenceMapper addressPersistenceMapper) {
        this.addressJpaRepository = addressJpaRepository;
        this.addressPersistenceMapper = addressPersistenceMapper;
    }

    @Override
    public Address save(UUID userId, Address address) {
        return addressPersistenceMapper.toDomain(
                addressJpaRepository.saveAndFlush(addressPersistenceMapper.toEntity(userId, address)));
    }

    @Override
    public Optional<Address> findById(UUID id) {
        return addressJpaRepository.findById(id).map(addressPersistenceMapper::toDomain);
    }
}

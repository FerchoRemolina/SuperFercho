package com.superfercho.identity.infrastructure.persistence;

import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.application.port.OwnedAddress;
import com.superfercho.identity.domain.model.Address;
import com.superfercho.identity.domain.model.AddressStatus;
import com.superfercho.identity.infrastructure.persistence.mapper.AddressPersistenceMapper;
import com.superfercho.identity.infrastructure.persistence.repository.AddressJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
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
        try {
            return addressPersistenceMapper.toDomain(
                    addressJpaRepository.saveAndFlush(addressPersistenceMapper.toEntity(userId, address)));
        } catch (DataIntegrityViolationException exception) {
            throw IdentityConstraintViolationTranslator.translate(exception);
        }
    }

    @Override
    public Optional<Address> findById(UUID id) {
        return addressJpaRepository.findById(id).map(addressPersistenceMapper::toDomain);
    }

    @Override
    public Optional<OwnedAddress> findOwnedById(UUID id) {
        return addressJpaRepository
                .findById(id)
                .map(entity -> new OwnedAddress(entity.getUserId(), addressPersistenceMapper.toDomain(entity)));
    }

    @Override
    public Optional<Address> findActiveDefaultByUserId(UUID userId) {
        return addressJpaRepository
                .findByUserIdAndIsDefaultTrueAndStatus(userId, AddressStatus.ACTIVE)
                .map(addressPersistenceMapper::toDomain);
    }

    @Override
    public List<Address> findByUserId(UUID userId) {
        return addressJpaRepository.findByUserId(userId).stream()
                .map(addressPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteAllByUserId(UUID userId) {
        addressJpaRepository.deleteByUserId(userId);
    }
}

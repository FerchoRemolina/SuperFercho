package com.superfercho.identity.infrastructure.persistence;

import com.superfercho.identity.application.port.CustomerRecordRepository;
import com.superfercho.identity.domain.model.CustomerRecord;
import com.superfercho.identity.infrastructure.persistence.mapper.CustomerRecordPersistenceMapper;
import com.superfercho.identity.infrastructure.persistence.repository.CustomerRecordJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CustomerRecordPersistenceAdapter implements CustomerRecordRepository {

    private final CustomerRecordJpaRepository customerRecordJpaRepository;
    private final CustomerRecordPersistenceMapper customerRecordPersistenceMapper;

    public CustomerRecordPersistenceAdapter(
            CustomerRecordJpaRepository customerRecordJpaRepository,
            CustomerRecordPersistenceMapper customerRecordPersistenceMapper) {
        this.customerRecordJpaRepository = customerRecordJpaRepository;
        this.customerRecordPersistenceMapper = customerRecordPersistenceMapper;
    }

    @Override
    public CustomerRecord save(CustomerRecord customerRecord) {
        try {
            return customerRecordPersistenceMapper.toDomain(customerRecordJpaRepository.saveAndFlush(
                    customerRecordPersistenceMapper.toEntity(customerRecord)));
        } catch (DataIntegrityViolationException exception) {
            throw IdentityConstraintViolationTranslator.translate(exception);
        }
    }

    @Override
    public Optional<CustomerRecord> findById(UUID id) {
        return customerRecordJpaRepository.findById(id).map(customerRecordPersistenceMapper::toDomain);
    }

    @Override
    public Optional<CustomerRecord> findByDocument(String documentType, String documentNumber) {
        return customerRecordJpaRepository
                .findByDocumentTypeAndDocumentNumber(documentType, documentNumber)
                .map(customerRecordPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByDocument(String documentType, String documentNumber) {
        return customerRecordJpaRepository.existsByDocumentTypeAndDocumentNumber(
                documentType, documentNumber);
    }
}

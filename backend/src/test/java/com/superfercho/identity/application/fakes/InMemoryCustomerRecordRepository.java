package com.superfercho.identity.application.fakes;

import com.superfercho.identity.application.exception.DocumentAlreadyExistsException;
import com.superfercho.identity.application.port.CustomerRecordRepository;
import com.superfercho.identity.domain.model.CustomerRecord;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryCustomerRecordRepository implements CustomerRecordRepository {

    private final Map<UUID, CustomerRecord> byId = new LinkedHashMap<>();

    @Override
    public CustomerRecord save(CustomerRecord customerRecord) {
        boolean documentTaken = byId.values().stream()
                .anyMatch(existing -> existing.documentType().equals(customerRecord.documentType())
                        && existing.documentNumber().equals(customerRecord.documentNumber())
                        && !existing.id().equals(customerRecord.id()));
        if (documentTaken) {
            throw new DocumentAlreadyExistsException(
                    customerRecord.documentType(), customerRecord.documentNumber());
        }
        byId.put(customerRecord.id(), customerRecord);
        return customerRecord;
    }

    @Override
    public Optional<CustomerRecord> findById(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<CustomerRecord> findByDocument(String documentType, String documentNumber) {
        return byId.values().stream()
                .filter(record -> record.documentType().equals(documentType)
                        && record.documentNumber().equals(documentNumber))
                .findFirst();
    }

    @Override
    public boolean existsByDocument(String documentType, String documentNumber) {
        return findByDocument(documentType, documentNumber).isPresent();
    }
}

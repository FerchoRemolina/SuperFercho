package com.superfercho.identity.application.port;

import com.superfercho.identity.domain.model.CustomerRecord;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRecordRepository {

    CustomerRecord save(CustomerRecord customerRecord);

    Optional<CustomerRecord> findById(UUID id);

    Optional<CustomerRecord> findByDocument(String documentType, String documentNumber);

    boolean existsByDocument(String documentType, String documentNumber);
}

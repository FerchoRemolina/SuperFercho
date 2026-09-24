package com.superfercho.identity.application.port;

import com.superfercho.identity.domain.model.CustomerPreview;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerPreviewRepository {

    CustomerPreview save(CustomerPreview preview);

    Optional<CustomerPreview> findById(UUID id);

    Optional<CustomerPreview> findActiveByAdminUserId(UUID adminUserId);

    Optional<CustomerPreview> findByTemporaryCustomerId(UUID temporaryCustomerId);

    boolean existsByTemporaryCustomerId(UUID temporaryCustomerId);

    /**
     * Atomically transitions ACTIVE → CLOSED. Empty means another writer already closed it.
     */
    Optional<CustomerPreview> claimClose(UUID previewId, Instant closedAt);

    List<CustomerPreview> findExpiredActive(Instant now);
}

package com.superfercho.orders.application.port;

import com.superfercho.orders.application.dto.CheckoutRequestFingerprint;
import com.superfercho.orders.application.dto.CheckoutResult;
import com.superfercho.orders.application.dto.IdempotencyRecord;
import com.superfercho.orders.application.exception.IdempotencyConflictException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyPort {

    Optional<IdempotencyRecord> find(String key, UUID customerId);

    void save(IdempotencyRecord record);

    default Optional<CheckoutResult> validateReuse(
            String key, UUID customerId, CheckoutRequestFingerprint fingerprint, Instant now) {
        Optional<IdempotencyRecord> found = find(key, customerId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        IdempotencyRecord record = found.get();
        if (record.expiresAt() != null && now.isAfter(record.expiresAt())) {
            return Optional.empty();
        }
        if (!record.fingerprint().equals(fingerprint)) {
            throw new IdempotencyConflictException(key);
        }
        return Optional.of(record.result());
    }
}

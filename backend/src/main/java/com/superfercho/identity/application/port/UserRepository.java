package com.superfercho.identity.application.port;

import com.superfercho.identity.domain.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    /**
     * True if a non-deleted user currently owns the email ({@code deletedAt == null}).
     */
    boolean existsByEmail(String email);

    boolean existsByDocument(String documentType, String documentNumber);

    /**
     * Finds the live user ({@code deletedAt == null}) for the email, if any.
     */
    Optional<User> findByEmail(String email);

    Optional<User> findByDocument(String documentType, String documentNumber);

    /**
     * Finds the non-deleted user linked to the given customer record, if any.
     */
    Optional<User> findLiveByCustomerRecordId(UUID customerRecordId);

    /**
     * All operational accounts (live and soft-deleted) historically linked to the customer record.
     */
    List<User> findAllByCustomerRecordId(UUID customerRecordId);

    void deleteById(UUID id);
}

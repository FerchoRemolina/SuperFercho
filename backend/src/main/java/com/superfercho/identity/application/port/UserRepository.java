package com.superfercho.identity.application.port;

import com.superfercho.identity.domain.model.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    boolean existsByEmail(String email);

    boolean existsByDocument(String documentType, String documentNumber);

    Optional<User> findByEmail(String email);

    Optional<User> findByDocument(String documentType, String documentNumber);

    void deleteById(UUID id);
}

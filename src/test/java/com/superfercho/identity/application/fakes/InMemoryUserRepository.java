package com.superfercho.identity.application.fakes;

import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.model.User;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryUserRepository implements UserRepository {

    private final Map<UUID, User> users = new LinkedHashMap<>();

    @Override
    public User save(User user) {
        users.put(user.id(), user);
        return user;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public boolean existsByEmail(String email) {
        return users.values().stream().anyMatch(user -> user.email().equals(email));
    }

    @Override
    public boolean existsByDocument(String documentType, String documentNumber) {
        return users.values().stream()
                .anyMatch(user ->
                        user.documentType().equals(documentType)
                                && user.documentNumber().equals(documentNumber));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return users.values().stream().filter(user -> user.email().equals(email)).findFirst();
    }
}

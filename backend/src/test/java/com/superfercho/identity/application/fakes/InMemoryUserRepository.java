package com.superfercho.identity.application.fakes;

import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.model.User;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
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
    public List<User> findAllByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().map(users::get).filter(user -> user != null).toList();
    }

    @Override
    public boolean existsByEmail(String email) {
        return users.values().stream()
                .anyMatch(user -> user.deletedAt() == null && user.email().equals(email));
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
        return users.values().stream()
                .filter(user -> user.deletedAt() == null && user.email().equals(email))
                .findFirst();
    }

    @Override
    public Optional<User> findByDocument(String documentType, String documentNumber) {
        return users.values().stream()
                .filter(user ->
                        user.documentType().equals(documentType)
                                && user.documentNumber().equals(documentNumber))
                .findFirst();
    }

    @Override
    public Optional<User> findLiveByCustomerRecordId(UUID customerRecordId) {
        return users.values().stream()
                .filter(user -> user.deletedAt() == null
                        && customerRecordId.equals(user.customerRecordId()))
                .findFirst();
    }

    @Override
    public List<User> findAllByCustomerRecordId(UUID customerRecordId) {
        return users.values().stream()
                .filter(user -> customerRecordId.equals(user.customerRecordId()))
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        users.remove(id);
    }
}

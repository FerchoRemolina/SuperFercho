package com.superfercho.identity.infrastructure.persistence;

import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.superfercho.identity.infrastructure.persistence.repository.UserJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final UserPersistenceMapper userPersistenceMapper;

    public UserPersistenceAdapter(
            UserJpaRepository userJpaRepository, UserPersistenceMapper userPersistenceMapper) {
        this.userJpaRepository = userJpaRepository;
        this.userPersistenceMapper = userPersistenceMapper;
    }

    @Override
    public User save(User user) {
        return userPersistenceMapper.toDomain(
                userJpaRepository.saveAndFlush(userPersistenceMapper.toEntity(user)));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id).map(userPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByDocument(String documentType, String documentNumber) {
        return userJpaRepository.existsByDocumentTypeAndDocumentNumber(documentType, documentNumber);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(userPersistenceMapper::toDomain);
    }
}

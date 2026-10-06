package com.superfercho.identity.infrastructure.persistence;

import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.superfercho.identity.infrastructure.persistence.repository.UserJpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
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
        try {
            return userPersistenceMapper.toDomain(
                    userJpaRepository.saveAndFlush(userPersistenceMapper.toEntity(user)));
        } catch (DataIntegrityViolationException exception) {
            throw IdentityConstraintViolationTranslator.translate(exception);
        }
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id).map(userPersistenceMapper::toDomain);
    }

    @Override
    public List<User> findAllByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return userJpaRepository.findAllById(ids).stream()
                .map(userPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmailAndDeletedAtIsNull(email);
    }

    @Override
    public boolean existsByDocument(String documentType, String documentNumber) {
        return userJpaRepository.existsByDocumentTypeAndDocumentNumber(documentType, documentNumber);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmailAndDeletedAtIsNull(email).map(userPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByDocument(String documentType, String documentNumber) {
        return userJpaRepository
                .findByDocumentTypeAndDocumentNumber(documentType, documentNumber)
                .map(userPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findLiveByCustomerRecordId(UUID customerRecordId) {
        return userJpaRepository
                .findByCustomerRecordIdAndDeletedAtIsNull(customerRecordId)
                .map(userPersistenceMapper::toDomain);
    }

    @Override
    public List<User> findAllByCustomerRecordId(UUID customerRecordId) {
        return userJpaRepository.findAllByCustomerRecordIdOrderByCreatedAtDescIdAsc(customerRecordId).stream()
                .map(userPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        userJpaRepository.deleteById(id);
        userJpaRepository.flush();
    }
}

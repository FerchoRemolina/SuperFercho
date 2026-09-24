package com.superfercho.identity.infrastructure.persistence;

import com.superfercho.identity.application.port.CustomerPreviewRepository;
import com.superfercho.identity.domain.model.CustomerPreview;
import com.superfercho.identity.domain.model.CustomerPreviewStatus;
import com.superfercho.identity.infrastructure.persistence.mapper.CustomerPreviewPersistenceMapper;
import com.superfercho.identity.infrastructure.persistence.repository.CustomerPreviewJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
public class CustomerPreviewPersistenceAdapter implements CustomerPreviewRepository {

    private final CustomerPreviewJpaRepository repository;

    public CustomerPreviewPersistenceAdapter(CustomerPreviewJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public CustomerPreview save(CustomerPreview preview) {
        return CustomerPreviewPersistenceMapper.toDomain(
                repository.saveAndFlush(CustomerPreviewPersistenceMapper.toEntity(preview)));
    }

    @Override
    public Optional<CustomerPreview> findById(UUID id) {
        return repository.findById(id).map(CustomerPreviewPersistenceMapper::toDomain);
    }

    @Override
    public Optional<CustomerPreview> findActiveByAdminUserId(UUID adminUserId) {
        return repository
                .findByAdminUserIdAndStatus(adminUserId, CustomerPreviewStatus.ACTIVE)
                .map(CustomerPreviewPersistenceMapper::toDomain);
    }

    @Override
    public Optional<CustomerPreview> findByTemporaryCustomerId(UUID temporaryCustomerId) {
        return repository
                .findByTemporaryCustomerId(temporaryCustomerId)
                .map(CustomerPreviewPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByTemporaryCustomerId(UUID temporaryCustomerId) {
        return repository.existsByTemporaryCustomerId(temporaryCustomerId);
    }

    @Override
    @Transactional
    public Optional<CustomerPreview> claimClose(UUID previewId, Instant closedAt) {
        int updated = repository.claimClose(previewId, closedAt);
        if (updated == 0) {
            return Optional.empty();
        }
        return repository.findById(previewId).map(CustomerPreviewPersistenceMapper::toDomain);
    }

    @Override
    public List<CustomerPreview> findExpiredActive(Instant now) {
        return repository.findExpiredActive(now).stream()
                .map(CustomerPreviewPersistenceMapper::toDomain)
                .toList();
    }
}

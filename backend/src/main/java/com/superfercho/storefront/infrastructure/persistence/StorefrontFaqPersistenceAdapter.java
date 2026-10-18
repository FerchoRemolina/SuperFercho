package com.superfercho.storefront.infrastructure.persistence;

import com.superfercho.storefront.application.port.StorefrontFaqRepository;
import com.superfercho.storefront.domain.model.StorefrontFaq;
import com.superfercho.storefront.infrastructure.persistence.entity.StorefrontFaqJpaEntity;
import com.superfercho.storefront.infrastructure.persistence.repository.StorefrontFaqJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
public class StorefrontFaqPersistenceAdapter implements StorefrontFaqRepository {

    private final StorefrontFaqJpaRepository jpaRepository;

    public StorefrontFaqPersistenceAdapter(StorefrontFaqJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorefrontFaq> findAllOrdered() {
        return jpaRepository.findAllByOrderByDisplayOrderAscCreatedAtAsc().stream()
                .map(StorefrontFaqPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorefrontFaq> findActiveOrdered() {
        return jpaRepository.findAllByActiveTrueOrderByDisplayOrderAscCreatedAtAsc().stream()
                .map(StorefrontFaqPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StorefrontFaq> findById(UUID id) {
        return jpaRepository.findById(id).map(StorefrontFaqPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional
    public StorefrontFaq save(StorefrontFaq faq) {
        StorefrontFaqJpaEntity entity = jpaRepository
                .findById(faq.id())
                .map(existing -> new StorefrontFaqJpaEntity(
                        existing.getId(),
                        faq.question(),
                        faq.answer(),
                        faq.displayOrder(),
                        faq.active(),
                        existing.getCreatedAt(),
                        faq.updatedAt()))
                .orElseGet(() -> new StorefrontFaqJpaEntity(
                        faq.id(),
                        faq.question(),
                        faq.answer(),
                        faq.displayOrder(),
                        faq.active(),
                        faq.createdAt(),
                        faq.updatedAt()));
        return toDomain(jpaRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private static StorefrontFaq toDomain(StorefrontFaqJpaEntity entity) {
        return new StorefrontFaq(
                entity.getId(),
                entity.getQuestion(),
                entity.getAnswer(),
                entity.getDisplayOrder(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}

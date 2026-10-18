package com.superfercho.storefront.infrastructure.persistence;

import com.superfercho.storefront.application.port.StorefrontContentRepository;
import com.superfercho.storefront.domain.model.StorefrontContent;
import com.superfercho.storefront.infrastructure.persistence.entity.StorefrontContentJpaEntity;
import com.superfercho.storefront.infrastructure.persistence.repository.StorefrontContentJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
public class StorefrontContentPersistenceAdapter implements StorefrontContentRepository {

    private final StorefrontContentJpaRepository jpaRepository;

    public StorefrontContentPersistenceAdapter(StorefrontContentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StorefrontContent> find() {
        return jpaRepository.findAll().stream().findFirst().map(StorefrontContentPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional
    public StorefrontContent save(
            StorefrontContent content, UUID updatedBy, Instant updatedAt) {
        StorefrontContentJpaEntity entity = jpaRepository
                .findAll()
                .stream()
                .findFirst()
                .map(existing -> updateEntity(existing, content, updatedBy, updatedAt))
                .orElseGet(() -> createEntity(content, updatedBy, updatedAt));
        return toDomain(jpaRepository.saveAndFlush(entity));
    }

    private static StorefrontContentJpaEntity updateEntity(
            StorefrontContentJpaEntity existing,
            StorefrontContent content,
            UUID updatedBy,
            Instant updatedAt) {
        return new StorefrontContentJpaEntity(
                existing.getId(),
                content.heroTitle(),
                content.heroDescription(),
                content.heroCtaLabel(),
                content.heroImageUrl(),
                content.heroMode(),
                content.supportEmail(),
                content.linkedinUrl(),
                content.githubUrl(),
                content.qrImageUrl(),
                updatedBy,
                existing.getCreatedAt(),
                updatedAt);
    }

    private static StorefrontContentJpaEntity createEntity(
            StorefrontContent content, UUID updatedBy, Instant updatedAt) {
        return new StorefrontContentJpaEntity(
                StorefrontContentJpaEntity.SINGLETON_ID,
                content.heroTitle(),
                content.heroDescription(),
                content.heroCtaLabel(),
                content.heroImageUrl(),
                content.heroMode(),
                content.supportEmail(),
                content.linkedinUrl(),
                content.githubUrl(),
                content.qrImageUrl(),
                updatedBy,
                updatedAt,
                updatedAt);
    }

    private static StorefrontContent toDomain(StorefrontContentJpaEntity entity) {
        return StorefrontContent.reconstitute(
                entity.getHeroTitle(),
                entity.getHeroDescription(),
                entity.getHeroCtaLabel(),
                entity.getHeroImageUrl(),
                entity.getHeroMode(),
                entity.getSupportEmail(),
                entity.getLinkedinUrl(),
                entity.getGithubUrl(),
                entity.getQrImageUrl(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt());
    }
}

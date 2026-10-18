package com.superfercho.storefront.infrastructure.persistence.repository;

import com.superfercho.storefront.infrastructure.persistence.entity.StorefrontFaqJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorefrontFaqJpaRepository
        extends JpaRepository<StorefrontFaqJpaEntity, UUID> {

    java.util.List<StorefrontFaqJpaEntity> findAllByOrderByDisplayOrderAscCreatedAtAsc();

    java.util.List<StorefrontFaqJpaEntity> findAllByActiveTrueOrderByDisplayOrderAscCreatedAtAsc();
}

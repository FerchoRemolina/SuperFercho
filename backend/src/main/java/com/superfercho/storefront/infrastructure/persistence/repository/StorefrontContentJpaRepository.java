package com.superfercho.storefront.infrastructure.persistence.repository;

import com.superfercho.storefront.infrastructure.persistence.entity.StorefrontContentJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorefrontContentJpaRepository
        extends JpaRepository<StorefrontContentJpaEntity, UUID> {}

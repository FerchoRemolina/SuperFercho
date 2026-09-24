package com.superfercho.shopping.infrastructure.persistence.repository;

import com.superfercho.shopping.infrastructure.persistence.entity.FavoriteJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface FavoriteJpaRepository extends JpaRepository<FavoriteJpaEntity, UUID> {

    Optional<FavoriteJpaEntity> findByCustomerIdAndProductId(UUID customerId, UUID productId);

    List<FavoriteJpaEntity> findAllByCustomerId(UUID customerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    void deleteByCustomerIdAndProductId(UUID customerId, UUID productId);
}

package com.superfercho.shopping.infrastructure.persistence.repository;

import com.superfercho.shopping.infrastructure.persistence.entity.ShoppingListJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShoppingListJpaRepository extends JpaRepository<ShoppingListJpaEntity, UUID> {

    List<ShoppingListJpaEntity> findAllByCustomerId(UUID customerId);
}

package com.superfercho.shopping.infrastructure.persistence.repository;

import com.superfercho.shopping.infrastructure.persistence.entity.ShoppingListJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ShoppingListJpaRepository extends JpaRepository<ShoppingListJpaEntity, UUID> {

    @Query(
            "SELECT s FROM ShoppingListJpaEntity s WHERE s.customerId = :customerId"
                    + " ORDER BY LOWER(s.name) ASC")
    List<ShoppingListJpaEntity> findAllByCustomerIdOrderByNameIgnoreCaseAsc(
            @Param("customerId") UUID customerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    void deleteAllByCustomerId(UUID customerId);
}

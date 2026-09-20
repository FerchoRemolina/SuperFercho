package com.superfercho.catalog.infrastructure.persistence.repository;

import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.catalog.infrastructure.persistence.entity.ProductJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {

    List<ProductJpaEntity> findByCategoryId(UUID categoryId);

    List<ProductJpaEntity> findByStatus(ProductStatus status);

    List<ProductJpaEntity> findByCategoryIdAndStatus(UUID categoryId, ProductStatus status);

    boolean existsByBarcode(String barcode);

    @Query(
            """
            select p from ProductJpaEntity p
            where lower(p.name) like lower(concat('%', :text, '%'))
               or (p.brand is not null and lower(p.brand) like lower(concat('%', :text, '%')))
               or (p.barcode is not null and lower(p.barcode) like lower(concat('%', :text, '%')))
            """)
    List<ProductJpaEntity> searchByNameBrandOrBarcode(@Param("text") String text);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value =
                    """
                    update catalog.products
                       set stock = stock - :quantity,
                           updated_at = :updatedAt
                     where id = :id
                       and status = 'ACTIVE'
                       and stock >= :quantity
                       and :quantity > 0
                    """,
            nativeQuery = true)
    int decrementStockIfAvailable(
            @Param("id") UUID id, @Param("quantity") int quantity, @Param("updatedAt") Instant updatedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value =
                    """
                    update catalog.products
                       set stock = stock + :quantity,
                           updated_at = :updatedAt
                     where id = :id
                       and :quantity > 0
                    """,
            nativeQuery = true)
    int incrementStock(
            @Param("id") UUID id, @Param("quantity") int quantity, @Param("updatedAt") Instant updatedAt);
}

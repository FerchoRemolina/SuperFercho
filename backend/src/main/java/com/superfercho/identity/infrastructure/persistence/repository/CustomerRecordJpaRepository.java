package com.superfercho.identity.infrastructure.persistence.repository;

import com.superfercho.identity.infrastructure.persistence.entity.CustomerRecordJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRecordJpaRepository extends JpaRepository<CustomerRecordJpaEntity, UUID> {

    boolean existsByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);

    Optional<CustomerRecordJpaEntity> findByDocumentTypeAndDocumentNumber(
            String documentType, String documentNumber);

    @Query(
            value =
                    """
                    SELECT date_trunc(CAST(:unit AS text), cr.created_at AT TIME ZONE 'America/Bogota')
                               AT TIME ZONE 'America/Bogota' AS bucket_start,
                           COUNT(*) AS customer_count
                      FROM identity.customer_records cr
                     WHERE cr.created_at >= :fromInclusive
                       AND cr.created_at < :toExclusive
                     GROUP BY 1
                     ORDER BY 1
                    """,
            nativeQuery = true)
    List<Object[]> countRegistrationsByBucket(
            @Param("unit") String unit,
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive);

    @Query(
            value =
                    """
                    SELECT COUNT(*)
                      FROM identity.customer_records cr
                     WHERE cr.created_at >= :fromInclusive
                       AND cr.created_at < :toExclusive
                    """,
            nativeQuery = true)
    long countRegistrationsBetween(
            @Param("fromInclusive") Instant fromInclusive, @Param("toExclusive") Instant toExclusive);
}

package com.superfercho.assistantops.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FerchoChatTurnJpaRepository extends JpaRepository<FerchoChatTurnEntity, UUID> {

    long countByUserIdAndStatusAndCreatedAtAfter(UUID userId, String status, Instant after);

    long countByUserIdAndStatusAndCreatedAtBetween(UUID userId, String status, Instant start, Instant end);

    long countByUserIdAndStatusStartingWithAndCreatedAtBetween(
            UUID userId, String status, Instant start, Instant end);

    long countByStatusAndCreatedAtBetween(String status, Instant start, Instant end);

    long countByStatusStartingWithAndCreatedAtBetween(String status, Instant start, Instant end);

    /** Serializa la decisión de límites por cliente dentro de la transacción. */
    @Query(value = "select pg_advisory_xact_lock(hashtextextended(:userId, 0))", nativeQuery = true)
    void acquireUserLock(@Param("userId") String userId);

    @Query("""
            select coalesce(sum(t.llmCalls), 0),
                   coalesce(sum(t.promptTokens), 0),
                   coalesce(sum(t.completionTokens), 0)
            from FerchoChatTurnEntity t
            where t.userId = :userId and t.status = 'ACCEPTED'
              and t.createdAt >= :windowStart and t.createdAt < :windowEnd
            """)
    List<Object[]> sumWindowForUser(@Param("userId") UUID userId, @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd);

    List<FerchoChatTurnEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("""
            select coalesce(sum(t.llmCalls), 0),
                   coalesce(sum(t.promptTokens), 0),
                   coalesce(sum(t.completionTokens), 0)
            from FerchoChatTurnEntity t
            where t.createdAt >= :windowStart and t.createdAt < :windowEnd
            """)
    List<Object[]> sumWindowGlobal(
            @Param("windowStart") Instant windowStart, @Param("windowEnd") Instant windowEnd);
}

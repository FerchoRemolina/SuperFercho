package com.superfercho.assistantops.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FerchoAdminAlertJpaRepository extends JpaRepository<FerchoAdminAlertEntity, UUID> {

    boolean existsByUserIdAndStatus(UUID userId, String status);

    List<FerchoAdminAlertEntity> findByStatusOrderByCreatedAtDesc(String status);

    List<FerchoAdminAlertEntity> findTop50ByOrderByCreatedAtDesc();

    List<FerchoAdminAlertEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByStatus(String status);

    long countByUserIdAndStatus(UUID userId, String status);
}

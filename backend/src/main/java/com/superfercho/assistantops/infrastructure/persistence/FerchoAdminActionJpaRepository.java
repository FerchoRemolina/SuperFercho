package com.superfercho.assistantops.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FerchoAdminActionJpaRepository extends JpaRepository<FerchoAdminActionEntity, UUID> {

    List<FerchoAdminActionEntity> findTop50ByTargetUserIdOrderByCreatedAtDesc(UUID targetUserId);
}

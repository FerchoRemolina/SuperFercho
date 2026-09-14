package com.superfercho.identity.infrastructure.persistence.repository;

import com.superfercho.identity.infrastructure.persistence.entity.UserJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {
}

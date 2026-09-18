package com.superfercho.knowledge.infrastructure.persistence.repository;

import com.superfercho.knowledge.infrastructure.persistence.entity.KnowledgeDocumentJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeDocumentJpaRepository extends JpaRepository<KnowledgeDocumentJpaEntity, UUID> {}

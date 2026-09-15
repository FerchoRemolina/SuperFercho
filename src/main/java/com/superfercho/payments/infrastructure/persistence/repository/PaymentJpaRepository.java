package com.superfercho.payments.infrastructure.persistence.repository;

import com.superfercho.payments.infrastructure.persistence.entity.PaymentJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {}

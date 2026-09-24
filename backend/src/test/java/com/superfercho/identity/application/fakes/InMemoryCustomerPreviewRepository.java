package com.superfercho.identity.application.fakes;

import com.superfercho.identity.application.port.CustomerPreviewRepository;
import com.superfercho.identity.domain.model.CustomerPreview;
import com.superfercho.identity.domain.model.CustomerPreviewStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryCustomerPreviewRepository implements CustomerPreviewRepository {

    private final Map<UUID, CustomerPreview> byId = new ConcurrentHashMap<>();

    @Override
    public CustomerPreview save(CustomerPreview preview) {
        if (preview.status() == CustomerPreviewStatus.ACTIVE) {
            boolean conflict = byId.values().stream()
                    .anyMatch(existing -> existing.status() == CustomerPreviewStatus.ACTIVE
                            && existing.adminUserId().equals(preview.adminUserId())
                            && !existing.id().equals(preview.id()));
            if (conflict) {
                throw new IllegalStateException("active preview already exists for admin");
            }
        }
        byId.put(preview.id(), preview);
        return preview;
    }

    @Override
    public Optional<CustomerPreview> findById(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<CustomerPreview> findActiveByAdminUserId(UUID adminUserId) {
        return byId.values().stream()
                .filter(preview -> preview.adminUserId().equals(adminUserId))
                .filter(preview -> preview.status() == CustomerPreviewStatus.ACTIVE)
                .findFirst();
    }

    @Override
    public Optional<CustomerPreview> findByTemporaryCustomerId(UUID temporaryCustomerId) {
        return byId.values().stream()
                .filter(preview -> preview.temporaryCustomerId().equals(temporaryCustomerId))
                .findFirst();
    }

    @Override
    public boolean existsByTemporaryCustomerId(UUID temporaryCustomerId) {
        return findByTemporaryCustomerId(temporaryCustomerId).isPresent();
    }

    @Override
    public Optional<CustomerPreview> claimClose(UUID previewId, Instant closedAt) {
        CustomerPreview current = byId.get(previewId);
        if (current == null || current.status() != CustomerPreviewStatus.ACTIVE) {
            return Optional.empty();
        }
        CustomerPreview closed = current.close(closedAt);
        byId.put(previewId, closed);
        return Optional.of(closed);
    }

    @Override
    public List<CustomerPreview> findExpiredActive(Instant now) {
        List<CustomerPreview> expired = new ArrayList<>();
        for (CustomerPreview preview : byId.values()) {
            if (preview.status() == CustomerPreviewStatus.ACTIVE && preview.isExpired(now)) {
                expired.add(preview);
            }
        }
        return List.copyOf(expired);
    }
}

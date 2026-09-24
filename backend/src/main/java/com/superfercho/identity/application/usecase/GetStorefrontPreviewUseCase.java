package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.StorefrontPreviewStatusResult;
import com.superfercho.identity.application.exception.StorefrontPreviewForbiddenException;
import com.superfercho.identity.application.exception.StorefrontPreviewNotFoundException;
import com.superfercho.identity.application.port.CurrentUserProvider;
import com.superfercho.identity.application.port.CustomerPreviewRepository;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.model.CustomerPreview;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class GetStorefrontPreviewUseCase {

    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;
    private final CustomerPreviewRepository customerPreviewRepository;
    private final FinalizeStorefrontPreviewUseCase finalizeStorefrontPreviewUseCase;
    private final Clock clock;

    public GetStorefrontPreviewUseCase(
            CurrentUserProvider currentUserProvider,
            UserRepository userRepository,
            CustomerPreviewRepository customerPreviewRepository,
            FinalizeStorefrontPreviewUseCase finalizeStorefrontPreviewUseCase,
            Clock clock) {
        this.currentUserProvider = currentUserProvider;
        this.userRepository = userRepository;
        this.customerPreviewRepository = customerPreviewRepository;
        this.finalizeStorefrontPreviewUseCase = finalizeStorefrontPreviewUseCase;
        this.clock = clock;
    }

    public StorefrontPreviewStatusResult execute() {
        UUID adminUserId = requireAdmin();
        Instant now = clock.instant();
        CustomerPreview preview = customerPreviewRepository
                .findActiveByAdminUserId(adminUserId)
                .orElseThrow(StorefrontPreviewNotFoundException::new);
        if (preview.isExpired(now)) {
            finalizeStorefrontPreviewUseCase.execute(preview.id());
            throw new StorefrontPreviewNotFoundException();
        }
        long remaining = Math.max(0, Duration.between(now, preview.expiresAt()).getSeconds());
        return new StorefrontPreviewStatusResult(
                preview.id(),
                preview.adminUserId(),
                preview.temporaryCustomerId(),
                preview.status(),
                preview.createdAt(),
                preview.expiresAt(),
                remaining,
                true);
    }

    private UUID requireAdmin() {
        UUID adminUserId = currentUserProvider.getCurrentUserId();
        User admin = userRepository
                .findById(adminUserId)
                .orElseThrow(() -> new StorefrontPreviewForbiddenException("admin user not found"));
        if (admin.role() != Role.ADMIN) {
            throw new StorefrontPreviewForbiddenException("only ADMIN can access storefront preview");
        }
        return adminUserId;
    }
}

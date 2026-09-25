package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.StorefrontPreviewSessionResult;
import com.superfercho.identity.application.exception.StorefrontPreviewForbiddenException;
import com.superfercho.identity.application.port.AccessTokenIssuer;
import com.superfercho.identity.application.port.CurrentUserProvider;
import com.superfercho.identity.application.port.CustomerPreviewRepository;
import com.superfercho.identity.application.port.IssuedAccessToken;
import com.superfercho.identity.application.port.PasswordHasher;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.model.CustomerPreview;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class StartStorefrontPreviewUseCase {

    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;
    private final CustomerPreviewRepository customerPreviewRepository;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer accessTokenIssuer;
    private final FinalizeStorefrontPreviewUseCase finalizeStorefrontPreviewUseCase;
    private final Clock clock;

    public StartStorefrontPreviewUseCase(
            CurrentUserProvider currentUserProvider,
            UserRepository userRepository,
            CustomerPreviewRepository customerPreviewRepository,
            PasswordHasher passwordHasher,
            AccessTokenIssuer accessTokenIssuer,
            FinalizeStorefrontPreviewUseCase finalizeStorefrontPreviewUseCase,
            Clock clock) {
        this.currentUserProvider = currentUserProvider;
        this.userRepository = userRepository;
        this.customerPreviewRepository = customerPreviewRepository;
        this.passwordHasher = passwordHasher;
        this.accessTokenIssuer = accessTokenIssuer;
        this.finalizeStorefrontPreviewUseCase = finalizeStorefrontPreviewUseCase;
        this.clock = clock;
    }

    public StorefrontPreviewSessionResult execute() {
        UUID adminUserId = currentUserProvider.getCurrentUserId();
        User admin = userRepository
                .findById(adminUserId)
                .orElseThrow(() -> new StorefrontPreviewForbiddenException("admin user not found"));
        if (admin.role() != Role.ADMIN) {
            throw new StorefrontPreviewForbiddenException("only ADMIN can start storefront preview");
        }

        Instant now = clock.instant();
        Optional<CustomerPreview> existing = customerPreviewRepository.findActiveByAdminUserId(adminUserId);
        if (existing.isPresent()) {
            CustomerPreview preview = existing.get();
            if (preview.isExpired(now)) {
                finalizeStorefrontPreviewUseCase.execute(preview.id());
            } else {
                return toSession(preview, now);
            }
        }

        return createNewPreview(admin, now);
    }

    private StorefrontPreviewSessionResult createNewPreview(User admin, Instant now) {
        UUID temporaryCustomerId = UUID.randomUUID();
        String suffix = temporaryCustomerId.toString().replace("-", "").substring(0, 12);
        User temporaryCustomer = User.create(
                temporaryCustomerId,
                "CC",
                "PREV" + suffix,
                "Preview Customer",
                "preview+" + suffix + "@temp.superfercho.local",
                "0000000000",
                passwordHasher.hash(UUID.randomUUID().toString()),
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                now,
                now);
        userRepository.save(temporaryCustomer);

        CustomerPreview preview =
                CustomerPreview.start(UUID.randomUUID(), admin.id(), temporaryCustomerId, now);
        try {
            customerPreviewRepository.save(preview);
        } catch (RuntimeException ex) {
            Optional<CustomerPreview> raced =
                    customerPreviewRepository.findActiveByAdminUserId(admin.id());
            if (raced.isPresent() && raced.get().isUsable(now)) {
                return toSession(raced.get(), now);
            }
            throw ex;
        }
        return toSession(preview, now);
    }

    private StorefrontPreviewSessionResult toSession(CustomerPreview preview, Instant now) {
        IssuedAccessToken customerToken =
                accessTokenIssuer.issue(preview.temporaryCustomerId(), Role.CUSTOMER, preview.id());
        // Fresh Admin access token (same issuer as login) so stash survives the full preview window.
        // Not a refresh token: Admin is already authenticated on this request.
        IssuedAccessToken adminToken = accessTokenIssuer.issue(preview.adminUserId(), Role.ADMIN);
        long remaining = Math.max(0, Duration.between(now, preview.expiresAt()).getSeconds());
        return new StorefrontPreviewSessionResult(
                preview.id(),
                preview.adminUserId(),
                preview.temporaryCustomerId(),
                Role.CUSTOMER,
                preview.status(),
                preview.createdAt(),
                preview.expiresAt(),
                remaining,
                customerToken.token(),
                customerToken.expiresAt(),
                adminToken.token(),
                adminToken.expiresAt());
    }
}

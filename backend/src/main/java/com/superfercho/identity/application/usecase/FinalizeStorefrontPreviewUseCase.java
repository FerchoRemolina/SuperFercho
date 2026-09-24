package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.application.port.CustomerPreviewRepository;
import com.superfercho.identity.application.port.PreviewAssistantCleanupPort;
import com.superfercho.identity.application.port.PreviewOrdersCleanupPort;
import com.superfercho.identity.application.port.PreviewShoppingCleanupPort;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.model.CustomerPreview;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Idempotent finalization: only the writer that successfully claims ACTIVE→CLOSED runs cleanup.
 */
public final class FinalizeStorefrontPreviewUseCase {

    private final CustomerPreviewRepository customerPreviewRepository;
    private final PreviewOrdersCleanupPort previewOrdersCleanupPort;
    private final PreviewShoppingCleanupPort previewShoppingCleanupPort;
    private final PreviewAssistantCleanupPort previewAssistantCleanupPort;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public FinalizeStorefrontPreviewUseCase(
            CustomerPreviewRepository customerPreviewRepository,
            PreviewOrdersCleanupPort previewOrdersCleanupPort,
            PreviewShoppingCleanupPort previewShoppingCleanupPort,
            PreviewAssistantCleanupPort previewAssistantCleanupPort,
            AddressRepository addressRepository,
            UserRepository userRepository,
            Clock clock) {
        this.customerPreviewRepository = customerPreviewRepository;
        this.previewOrdersCleanupPort = previewOrdersCleanupPort;
        this.previewShoppingCleanupPort = previewShoppingCleanupPort;
        this.previewAssistantCleanupPort = previewAssistantCleanupPort;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /**
     * @return true if this caller performed cleanup; false if already closed by another writer
     */
    public boolean execute(UUID previewId) {
        Instant now = clock.instant();
        Optional<CustomerPreview> claimed = customerPreviewRepository.claimClose(previewId, now);
        if (claimed.isEmpty()) {
            return false;
        }
        CustomerPreview preview = claimed.get();
        UUID temporaryCustomerId = preview.temporaryCustomerId();
        previewOrdersCleanupPort.cancelAndDeleteAllForCustomer(temporaryCustomerId);
        previewShoppingCleanupPort.deleteAllForCustomer(temporaryCustomerId);
        addressRepository.deleteAllByUserId(temporaryCustomerId);
        previewAssistantCleanupPort.deleteAllForUser(temporaryCustomerId);
        deactivateTemporaryUser(temporaryCustomerId, now);
        return true;
    }

    private void deactivateTemporaryUser(UUID temporaryCustomerId, Instant now) {
        userRepository
                .findById(temporaryCustomerId)
                .ifPresent(user -> {
                    if (user.status() == UserStatus.INACTIVE) {
                        return;
                    }
                    User deactivated = User.create(
                            user.id(),
                            user.documentType(),
                            user.documentNumber(),
                            user.fullName(),
                            user.email(),
                            user.phone(),
                            user.passwordHash(),
                            user.role(),
                            UserStatus.INACTIVE,
                            user.createdAt(),
                            now);
                    userRepository.save(deactivated);
                });
    }
}

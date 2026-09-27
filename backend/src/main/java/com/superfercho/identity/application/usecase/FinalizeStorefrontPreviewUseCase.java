package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.application.port.CustomerPreviewRepository;
import com.superfercho.identity.application.port.PasswordRecoveryTokenRepository;
import com.superfercho.identity.application.port.PreviewAssistantCleanupPort;
import com.superfercho.identity.application.port.PreviewOrdersCleanupPort;
import com.superfercho.identity.application.port.PreviewShoppingCleanupPort;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.model.CustomerPreview;
import com.superfercho.identity.domain.model.CustomerPreviewStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Retriable finalization of a storefront preview.
 *
 * <p>ACTIVE → CLOSED is claimed when possible; a CLOSED preview with leftover data can be cleaned
 * again. Cleanup is idempotent (delete-if-present). Infrastructure should wrap {@link #execute}
 * in a single local transaction so a mid-cleanup failure rolls back the claim as well.
 */
public class FinalizeStorefrontPreviewUseCase {

    private final CustomerPreviewRepository customerPreviewRepository;
    private final PreviewOrdersCleanupPort previewOrdersCleanupPort;
    private final PreviewShoppingCleanupPort previewShoppingCleanupPort;
    private final PreviewAssistantCleanupPort previewAssistantCleanupPort;
    private final PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public FinalizeStorefrontPreviewUseCase(
            CustomerPreviewRepository customerPreviewRepository,
            PreviewOrdersCleanupPort previewOrdersCleanupPort,
            PreviewShoppingCleanupPort previewShoppingCleanupPort,
            PreviewAssistantCleanupPort previewAssistantCleanupPort,
            PasswordRecoveryTokenRepository passwordRecoveryTokenRepository,
            AddressRepository addressRepository,
            UserRepository userRepository,
            Clock clock) {
        this.customerPreviewRepository = customerPreviewRepository;
        this.previewOrdersCleanupPort = previewOrdersCleanupPort;
        this.previewShoppingCleanupPort = previewShoppingCleanupPort;
        this.previewAssistantCleanupPort = previewAssistantCleanupPort;
        this.passwordRecoveryTokenRepository = passwordRecoveryTokenRepository;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /**
     * @return true if this caller performed (or completed) cleanup; false if the preview is already
     *     gone or another ACTIVE writer owns the claim
     */
    public boolean execute(UUID previewId) {
        Instant now = clock.instant();
        Optional<CustomerPreview> preview = resolveForCleanup(previewId, now);
        if (preview.isEmpty()) {
            return false;
        }
        CustomerPreview toClean = preview.get();
        UUID temporaryCustomerId = toClean.temporaryCustomerId();
        previewOrdersCleanupPort.cancelAndDeleteAllForCustomer(temporaryCustomerId);
        previewShoppingCleanupPort.deleteAllForCustomer(temporaryCustomerId);
        passwordRecoveryTokenRepository.deleteAllByUserId(temporaryCustomerId);
        addressRepository.deleteAllByUserId(temporaryCustomerId);
        previewAssistantCleanupPort.deleteAllForUser(temporaryCustomerId);
        customerPreviewRepository.deleteById(toClean.id());
        userRepository.deleteById(temporaryCustomerId);
        return true;
    }

    private Optional<CustomerPreview> resolveForCleanup(UUID previewId, Instant now) {
        Optional<CustomerPreview> claimed = customerPreviewRepository.claimClose(previewId, now);
        if (claimed.isPresent()) {
            return claimed;
        }
        Optional<CustomerPreview> existing = customerPreviewRepository.findById(previewId);
        if (existing.isPresent() && existing.get().status() == CustomerPreviewStatus.CLOSED) {
            return existing;
        }
        return Optional.empty();
    }
}

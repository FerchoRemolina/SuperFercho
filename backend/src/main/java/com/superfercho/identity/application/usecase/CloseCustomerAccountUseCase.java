package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.exception.CloseCustomerAccountForbiddenException;
import com.superfercho.identity.application.exception.UserNotFoundException;
import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.application.port.CurrentUserProvider;
import com.superfercho.identity.application.port.CustomerAccountAssistantCleanupPort;
import com.superfercho.identity.application.port.CustomerAccountCheckoutCleanupPort;
import com.superfercho.identity.application.port.CustomerAccountShoppingCleanupPort;
import com.superfercho.identity.application.port.CustomerPreviewRepository;
import com.superfercho.identity.application.port.PasswordRecoveryTokenRepository;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Soft-closes the authenticated CUSTOMER account: clears operational data and marks the User
 * INACTIVE with {@code deletedAt}. Preserves CustomerRecord, Orders, and Payments. Infrastructure
 * wraps {@link #execute} in a local transaction.
 */
public class CloseCustomerAccountUseCase {

    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;
    private final CustomerAccountShoppingCleanupPort shoppingCleanupPort;
    private final CustomerAccountCheckoutCleanupPort checkoutCleanupPort;
    private final CustomerAccountAssistantCleanupPort assistantCleanupPort;
    private final Optional<CustomerPreviewRepository> customerPreviewRepository;
    private final Clock clock;

    public CloseCustomerAccountUseCase(
            CurrentUserProvider currentUserProvider,
            UserRepository userRepository,
            AddressRepository addressRepository,
            PasswordRecoveryTokenRepository passwordRecoveryTokenRepository,
            CustomerAccountShoppingCleanupPort shoppingCleanupPort,
            CustomerAccountCheckoutCleanupPort checkoutCleanupPort,
            CustomerAccountAssistantCleanupPort assistantCleanupPort,
            Clock clock) {
        this(
                currentUserProvider,
                userRepository,
                addressRepository,
                passwordRecoveryTokenRepository,
                shoppingCleanupPort,
                checkoutCleanupPort,
                assistantCleanupPort,
                Optional.empty(),
                clock);
    }

    public CloseCustomerAccountUseCase(
            CurrentUserProvider currentUserProvider,
            UserRepository userRepository,
            AddressRepository addressRepository,
            PasswordRecoveryTokenRepository passwordRecoveryTokenRepository,
            CustomerAccountShoppingCleanupPort shoppingCleanupPort,
            CustomerAccountCheckoutCleanupPort checkoutCleanupPort,
            CustomerAccountAssistantCleanupPort assistantCleanupPort,
            CustomerPreviewRepository customerPreviewRepository,
            Clock clock) {
        this(
                currentUserProvider,
                userRepository,
                addressRepository,
                passwordRecoveryTokenRepository,
                shoppingCleanupPort,
                checkoutCleanupPort,
                assistantCleanupPort,
                Optional.ofNullable(customerPreviewRepository),
                clock);
    }

    private CloseCustomerAccountUseCase(
            CurrentUserProvider currentUserProvider,
            UserRepository userRepository,
            AddressRepository addressRepository,
            PasswordRecoveryTokenRepository passwordRecoveryTokenRepository,
            CustomerAccountShoppingCleanupPort shoppingCleanupPort,
            CustomerAccountCheckoutCleanupPort checkoutCleanupPort,
            CustomerAccountAssistantCleanupPort assistantCleanupPort,
            Optional<CustomerPreviewRepository> customerPreviewRepository,
            Clock clock) {
        this.currentUserProvider = currentUserProvider;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.passwordRecoveryTokenRepository = passwordRecoveryTokenRepository;
        this.shoppingCleanupPort = shoppingCleanupPort;
        this.checkoutCleanupPort = checkoutCleanupPort;
        this.assistantCleanupPort = assistantCleanupPort;
        this.customerPreviewRepository = customerPreviewRepository;
        this.clock = clock;
    }

    public void execute() {
        UUID userId = currentUserProvider.getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        if (user.role() != Role.CUSTOMER) {
            throw new CloseCustomerAccountForbiddenException(
                    "Only CUSTOMER accounts can close themselves");
        }
        if (user.customerRecordId() == null
                || customerPreviewRepository
                        .map(repository -> repository.existsByTemporaryCustomerId(userId))
                        .orElse(false)) {
            throw new CloseCustomerAccountForbiddenException(
                    "Storefront preview accounts cannot close a commercial customer account");
        }
        if (user.deletedAt() != null) {
            return;
        }

        Instant now = clock.instant();
        shoppingCleanupPort.deleteAllForCustomer(userId);
        checkoutCleanupPort.deleteCheckoutIdempotencyForCustomer(userId);
        passwordRecoveryTokenRepository.deleteAllByUserId(userId);
        addressRepository.deleteAllByUserId(userId);
        assistantCleanupPort.deleteAllForUser(userId);
        userRepository.save(user.closeAccount(now));
    }
}

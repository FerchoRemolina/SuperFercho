package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.CustomerProfileResult;
import com.superfercho.identity.application.dto.UpdateCustomerProfileCommand;
import com.superfercho.identity.application.exception.CustomerProfileForbiddenException;
import com.superfercho.identity.application.exception.CustomerRecordNotFoundException;
import com.superfercho.identity.application.exception.DocumentAlreadyExistsException;
import com.superfercho.identity.application.exception.UserAlreadyExistsException;
import com.superfercho.identity.application.exception.UserNotFoundException;
import com.superfercho.identity.application.port.CurrentUserProvider;
import com.superfercho.identity.application.port.CustomerPreviewRepository;
import com.superfercho.identity.application.port.CustomerRecordRepository;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.application.validation.CustomerRegistrationRules;
import com.superfercho.identity.application.validation.CustomerRegistrationRules.ValidatedRegistration;
import com.superfercho.identity.domain.model.CustomerRecord;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Actualiza el perfil del CUSTOMER autenticado: nombres, correo, teléfono y
 * documento. La identidad se resuelve en el backend vía
 * {@link CurrentUserProvider}. El documento vive en {@code User} y en el
 * {@link CustomerRecord} comercial vinculado: se actualiza en ambos de forma
 * consistente dentro de una misma transacción (la infraestructura envuelve
 * {@link #execute}). El identificador interno y el {@code customerRecordId} no
 * cambian, por lo que las compras conservan sus relaciones; no se alteran rol,
 * estado, contraseña ni datos de facturación históricos. Si el nuevo documento
 * ya pertenece a otro usuario vivo o a otro record comercial, se rechaza con
 * {@link DocumentAlreadyExistsException}.
 */
public class UpdateCustomerProfileUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCustomerProfileUseCase.class);

    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;
    private final CustomerRecordRepository customerRecordRepository;
    private final Optional<CustomerPreviewRepository> customerPreviewRepository;
    private final Clock clock;

    public UpdateCustomerProfileUseCase(
            CurrentUserProvider currentUserProvider,
            UserRepository userRepository,
            CustomerRecordRepository customerRecordRepository,
            Optional<CustomerPreviewRepository> customerPreviewRepository,
            Clock clock) {
        this.currentUserProvider = currentUserProvider;
        this.userRepository = userRepository;
        this.customerRecordRepository = customerRecordRepository;
        this.customerPreviewRepository = customerPreviewRepository;
        this.clock = clock;
    }

    public CustomerProfileResult execute(UpdateCustomerProfileCommand command) {
        UUID userId = currentUserProvider.getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        requireProfileEditable(user, userId);

        ValidatedRegistration validated = CustomerRegistrationRules.validateProfile(
                command.documentType(),
                command.documentNumber(),
                command.firstName(),
                command.lastName(),
                command.email(),
                command.phone());

        if (!user.email().equals(validated.email()) && userRepository.existsByEmail(validated.email())) {
            throw new UserAlreadyExistsException(validated.email());
        }

        boolean documentChanged =
                !user.documentType().equals(validated.documentType())
                        || !user.documentNumber().equals(validated.documentNumber());
        if (documentChanged) {
            requireDocumentAvailable(validated.documentType(), validated.documentNumber(), user);
        }

        CustomerRecord record = customerRecordRepository
                .findById(user.customerRecordId())
                .orElseThrow(() -> new CustomerRecordNotFoundException(user.customerRecordId()));

        Instant now = clock.instant();
        if (documentChanged) {
            customerRecordRepository.save(
                    record.updateDocument(validated.documentType(), validated.documentNumber(), now));
            LOGGER.info("Customer identification updated for user {}", userId);
        }

        User updated = user.updateProfile(
                validated.firstName(),
                validated.lastName(),
                validated.email(),
                validated.phone(),
                validated.documentType(),
                validated.documentNumber(),
                now);
        userRepository.save(updated);
        return CustomerProfileResult.from(updated);
    }

    private void requireProfileEditable(User user, UUID userId) {
        if (user.role() != Role.CUSTOMER
                || user.customerRecordId() == null
                || user.deletedAt() != null
                || customerPreviewRepository
                        .map(repository -> repository.existsByTemporaryCustomerId(userId))
                        .orElse(false)) {
            throw new CustomerProfileForbiddenException(userId);
        }
    }

    private void requireDocumentAvailable(String documentType, String documentNumber, User current) {
        customerRecordRepository
                .findByDocument(documentType, documentNumber)
                .filter(record -> !record.id().equals(current.customerRecordId()))
                .ifPresent(record -> {
                    throw new DocumentAlreadyExistsException(documentType, documentNumber);
                });
        userRepository
                .findByDocument(documentType, documentNumber)
                .filter(other -> !other.id().equals(current.id()))
                .ifPresent(other -> {
                    throw new DocumentAlreadyExistsException(documentType, documentNumber);
                });
    }
}

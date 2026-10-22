package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.CustomerProfileResult;
import com.superfercho.identity.application.exception.CustomerProfileForbiddenException;
import com.superfercho.identity.application.exception.UserNotFoundException;
import com.superfercho.identity.application.port.CurrentUserProvider;
import com.superfercho.identity.application.port.CustomerPreviewRepository;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import java.util.Optional;
import java.util.UUID;

/**
 * Consulta del perfil del CUSTOMER autenticado. La identidad se resuelve en el
 * backend vía {@link CurrentUserProvider}; las cuentas borradas o de vista
 * previa no pueden leer el perfil comercial.
 */
public class GetCustomerProfileUseCase {

    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;
    private final Optional<CustomerPreviewRepository> customerPreviewRepository;

    public GetCustomerProfileUseCase(
            CurrentUserProvider currentUserProvider,
            UserRepository userRepository,
            Optional<CustomerPreviewRepository> customerPreviewRepository) {
        this.currentUserProvider = currentUserProvider;
        this.userRepository = userRepository;
        this.customerPreviewRepository = customerPreviewRepository;
    }

    public CustomerProfileResult execute() {
        UUID userId = currentUserProvider.getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        requireProfileEditable(user, userId);
        return CustomerProfileResult.from(user);
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
}

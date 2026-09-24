package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.exception.StorefrontPreviewForbiddenException;
import com.superfercho.identity.application.exception.StorefrontPreviewNotFoundException;
import com.superfercho.identity.application.port.CurrentUserProvider;
import com.superfercho.identity.application.port.CustomerPreviewRepository;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.model.CustomerPreview;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import java.util.UUID;

public final class ExitStorefrontPreviewUseCase {

    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;
    private final CustomerPreviewRepository customerPreviewRepository;
    private final FinalizeStorefrontPreviewUseCase finalizeStorefrontPreviewUseCase;

    public ExitStorefrontPreviewUseCase(
            CurrentUserProvider currentUserProvider,
            UserRepository userRepository,
            CustomerPreviewRepository customerPreviewRepository,
            FinalizeStorefrontPreviewUseCase finalizeStorefrontPreviewUseCase) {
        this.currentUserProvider = currentUserProvider;
        this.userRepository = userRepository;
        this.customerPreviewRepository = customerPreviewRepository;
        this.finalizeStorefrontPreviewUseCase = finalizeStorefrontPreviewUseCase;
    }

    public void execute() {
        UUID adminUserId = currentUserProvider.getCurrentUserId();
        User admin = userRepository
                .findById(adminUserId)
                .orElseThrow(() -> new StorefrontPreviewForbiddenException("admin user not found"));
        if (admin.role() != Role.ADMIN) {
            throw new StorefrontPreviewForbiddenException("only ADMIN can exit storefront preview");
        }
        CustomerPreview preview = customerPreviewRepository
                .findActiveByAdminUserId(adminUserId)
                .orElseThrow(StorefrontPreviewNotFoundException::new);
        finalizeStorefrontPreviewUseCase.execute(preview.id());
    }
}

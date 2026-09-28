package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.PasswordRecoveryRequestResult;
import com.superfercho.identity.application.dto.RequestPasswordRecoveryByDocumentCommand;
import com.superfercho.identity.application.exception.PasswordRecoveryRateLimitedException;
import com.superfercho.identity.application.port.CustomerRecordRepository;
import com.superfercho.identity.application.port.PasswordRecoveryAbuseGuard;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.application.service.PasswordRecoveryIssuer;
import com.superfercho.identity.application.validation.CustomerRegistrationRules;
import com.superfercho.identity.domain.model.CustomerRecord;
import com.superfercho.identity.domain.model.User;
import java.util.Optional;

/**
 * Locates a LIVE CUSTOMER via CustomerRecord document and reuses the shared password-recovery
 * issuer. Always returns a generic public response.
 */
public final class RequestPasswordRecoveryByDocumentUseCase {

    private final CustomerRecordRepository customerRecordRepository;
    private final UserRepository userRepository;
    private final PasswordRecoveryIssuer passwordRecoveryIssuer;
    private final PasswordRecoveryAbuseGuard abuseGuard;

    public RequestPasswordRecoveryByDocumentUseCase(
            CustomerRecordRepository customerRecordRepository,
            UserRepository userRepository,
            PasswordRecoveryIssuer passwordRecoveryIssuer,
            PasswordRecoveryAbuseGuard abuseGuard) {
        this.customerRecordRepository = customerRecordRepository;
        this.userRepository = userRepository;
        this.passwordRecoveryIssuer = passwordRecoveryIssuer;
        this.abuseGuard = abuseGuard;
    }

    public PasswordRecoveryRequestResult execute(RequestPasswordRecoveryByDocumentCommand command) {
        CustomerRegistrationRules.requireDocument(command.documentType(), command.documentNumber());

        String clientIp = command.clientIp() == null ? "" : command.clientIp().trim();
        if (!abuseGuard.allowRequest(clientIp.isEmpty() ? "unknown" : clientIp)) {
            throw new PasswordRecoveryRateLimitedException();
        }

        Optional<CustomerRecord> record = customerRecordRepository.findByDocument(
                command.documentType(), command.documentNumber());
        if (record.isPresent()) {
            Optional<User> liveUser = userRepository.findLiveByCustomerRecordId(record.get().id());
            liveUser.ifPresent(user -> passwordRecoveryIssuer.issueForEligibleCustomer(user, clientIp));
        }

        return PasswordRecoveryRequestResult.genericForDocument();
    }
}

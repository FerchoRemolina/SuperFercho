package com.superfercho.identity.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.superfercho.identity.application.dto.CustomerProfileResult;
import com.superfercho.identity.application.dto.RegisterCustomerCommand;
import com.superfercho.identity.application.dto.RegisteredCustomer;
import com.superfercho.identity.application.exception.CustomerProfileForbiddenException;
import com.superfercho.identity.application.exception.UnauthenticatedUserException;
import com.superfercho.identity.application.fakes.FakePasswordHasher;
import com.superfercho.identity.application.fakes.InMemoryCustomerPreviewRepository;
import com.superfercho.identity.application.fakes.InMemoryCustomerRecordRepository;
import com.superfercho.identity.application.fakes.InMemoryUserRepository;
import com.superfercho.identity.application.port.CurrentUserProvider;
import com.superfercho.identity.domain.model.CustomerPreview;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCustomerProfileUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-04-01T15:00:00Z");
    private static final Instant CREATED = Instant.parse("2026-01-15T12:00:00Z");

    @Mock
    private CurrentUserProvider currentUserProvider;

    private InMemoryUserRepository users;
    private InMemoryCustomerPreviewRepository previews;
    private GetCustomerProfileUseCase getProfile;
    private RegisterCustomerUseCase registerCustomer;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        previews = new InMemoryCustomerPreviewRepository();
        getProfile = new GetCustomerProfileUseCase(currentUserProvider, users, Optional.of(previews));
        registerCustomer = new RegisterCustomerUseCase(
                users,
                new InMemoryCustomerRecordRepository(),
                new FakePasswordHasher(),
                Clock.fixed(CREATED, ZoneOffset.UTC));
    }

    @Test
    void shouldReturnOwnProfileWithoutSensitiveFields() {
        RegisteredCustomer registered = registerCustomer.execute(validRegistration().build());
        when(currentUserProvider.getCurrentUserId()).thenReturn(registered.id());

        CustomerProfileResult profile = getProfile.execute();

        assertEquals(registered.id(), profile.id());
        assertEquals("CC", profile.documentType());
        assertEquals("12345678", profile.documentNumber());
        assertEquals("Ada", profile.firstName());
        assertEquals("Lovelace", profile.lastName());
        assertEquals("ada@example.com", profile.email());
        assertEquals("3001234567", profile.phone());
        assertEquals(CREATED, profile.createdAt());
    }

    @Test
    void shouldRejectCustomerWithoutCustomerRecord() {
        UUID previewUserId = UUID.randomUUID();
        users.save(User.create(
                previewUserId,
                "CC",
                "PREV" + previewUserId.toString().replace("-", "").substring(0, 12),
                "Preview",
                "Temp",
                "preview+" + previewUserId + "@temp.superfercho.local",
                "3001111111",
                "hashed:pass",
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                CREATED,
                CREATED));
        when(currentUserProvider.getCurrentUserId()).thenReturn(previewUserId);

        assertThrows(CustomerProfileForbiddenException.class, getProfile::execute);
    }

    @Test
    void shouldRejectPreviewTemporaryCustomerTrackedInPreviewRepository() {
        RegisteredCustomer registered = registerCustomer.execute(validRegistration().build());
        previews.save(CustomerPreview.start(UUID.randomUUID(), UUID.randomUUID(), registered.id(), CREATED));
        when(currentUserProvider.getCurrentUserId()).thenReturn(registered.id());

        assertThrows(CustomerProfileForbiddenException.class, getProfile::execute);
    }

    @Test
    void shouldRejectClosedAccount() {
        RegisteredCustomer registered = registerCustomer.execute(validRegistration().build());
        User closed = users.findById(registered.id()).orElseThrow().closeAccount(NOW);
        users.save(closed);
        when(currentUserProvider.getCurrentUserId()).thenReturn(registered.id());

        assertThrows(CustomerProfileForbiddenException.class, getProfile::execute);
    }

    @Test
    void shouldPropagateUnauthenticatedFromCurrentUserProvider() {
        when(currentUserProvider.getCurrentUserId()).thenThrow(new UnauthenticatedUserException());

        assertThrows(UnauthenticatedUserException.class, getProfile::execute);
    }

    private static CommandBuilder validRegistration() {
        return new CommandBuilder();
    }

    private static final class CommandBuilder {
        private String documentType = "CC";
        private String documentNumber = "12345678";
        private String firstName = "Ada";
        private String lastName = "Lovelace";
        private String email = "ada@example.com";
        private String phone = "3001234567";
        private String password = "Luis123!";

        private CommandBuilder email(String email) {
            this.email = email;
            return this;
        }

        private CommandBuilder documentNumber(String documentNumber) {
            this.documentNumber = documentNumber;
            return this;
        }

        private RegisterCustomerCommand build() {
            return new RegisterCustomerCommand(
                    documentType, documentNumber, firstName, lastName, email, phone, password);
        }
    }
}

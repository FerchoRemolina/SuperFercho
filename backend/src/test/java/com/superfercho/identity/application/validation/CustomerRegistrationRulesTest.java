package com.superfercho.identity.application.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.superfercho.identity.application.dto.RegisterCustomerCommand;
import com.superfercho.identity.application.exception.InvalidRegistrationException;
import com.superfercho.identity.application.validation.CustomerRegistrationRules.ValidatedRegistration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CustomerRegistrationRulesTest {

    @Test
    void shouldAcceptValidRegistrationPayload() {
        ValidatedRegistration validated = CustomerRegistrationRules.validate(valid().build());

        assertEquals("CC", validated.documentType());
        assertEquals("12345678", validated.documentNumber());
        assertEquals("Ada", validated.firstName());
        assertEquals("Lovelace", validated.lastName());
        assertEquals("ada@example.com", validated.email());
        assertEquals("3001234567", validated.phone());
        assertEquals("Luis123!", validated.password());
    }

    @Test
    void shouldNormalizeFirstNameLastNameAndEmail() {
        ValidatedRegistration validated = CustomerRegistrationRules.validate(
                valid()
                        .firstName("  María José  ")
                        .lastName("  Albarín  ")
                        .email("  Ada@Example.COM ")
                        .build());

        assertEquals("María José", validated.firstName());
        assertEquals("Albarín", validated.lastName());
        assertEquals("ada@example.com", validated.email());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "TI"})
    void shouldRejectInvalidDocumentType(String documentType) {
        InvalidRegistrationException exception = assertThrows(
                InvalidRegistrationException.class,
                () -> CustomerRegistrationRules.validate(valid().documentType(documentType).build()));
        assertEquals("Selecciona un tipo de documento.", exception.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "12A", "12-34", "12 34"})
    void shouldRejectInvalidDocumentNumber(String documentNumber) {
        assertThrows(
                InvalidRegistrationException.class,
                () -> CustomerRegistrationRules.validate(
                        valid().documentNumber(documentNumber).build()));
    }

    @Test
    void shouldAcceptAccentedCompositeNamesUpToTwentyCharacters() {
        CustomerRegistrationRules.validate(valid().firstName("Luis Fernando").lastName("Remolina").build());
        CustomerRegistrationRules.validate(valid().firstName("María José").lastName("Albarín").build());
        CustomerRegistrationRules.validate(valid().firstName("Ñandú").lastName("Pérez").build());
        CustomerRegistrationRules.validate(
                valid().firstName("A".repeat(20)).lastName("B".repeat(20)).build());
    }

    @Test
    void shouldRejectFirstNameLongerThanTwentyCharacters() {
        InvalidRegistrationException exception = assertThrows(
                InvalidRegistrationException.class,
                () -> CustomerRegistrationRules.validate(valid().firstName("A".repeat(21)).build()));
        assertEquals("Introduce un nombre válido.", exception.getMessage());
    }

    @Test
    void shouldRejectLastNameLongerThanTwentyCharacters() {
        InvalidRegistrationException exception = assertThrows(
                InvalidRegistrationException.class,
                () -> CustomerRegistrationRules.validate(valid().lastName("A".repeat(21)).build()));
        assertEquals("Introduce un apellido válido.", exception.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void shouldRejectBlankFirstName(String firstName) {
        InvalidRegistrationException exception = assertThrows(
                InvalidRegistrationException.class,
                () -> CustomerRegistrationRules.validate(valid().firstName(firstName).build()));
        assertEquals("El nombre no puede quedar vacío.", exception.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void shouldRejectBlankLastName(String lastName) {
        InvalidRegistrationException exception = assertThrows(
                InvalidRegistrationException.class,
                () -> CustomerRegistrationRules.validate(valid().lastName(lastName).build()));
        assertEquals("El apellido no puede quedar vacío.", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ada@", "123", "Ada-Lovelace"})
    void shouldRejectInvalidFirstName(String firstName) {
        InvalidRegistrationException exception = assertThrows(
                InvalidRegistrationException.class,
                () -> CustomerRegistrationRules.validate(valid().firstName(firstName).build()));
        assertEquals("Introduce un nombre válido.", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Lovelace@", "123", "De-la-Cruz"})
    void shouldRejectInvalidLastName(String lastName) {
        InvalidRegistrationException exception = assertThrows(
                InvalidRegistrationException.class,
                () -> CustomerRegistrationRules.validate(valid().lastName(lastName).build()));
        assertEquals("Introduce un apellido válido.", exception.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "ada@", "not-an-email"})
    void shouldRejectInvalidEmail(String email) {
        assertThrows(
                InvalidRegistrationException.class,
                () -> CustomerRegistrationRules.validate(valid().email(email).build()));
    }

    @Test
    void shouldAcceptValidColombianMobileNumbers() {
        CustomerRegistrationRules.validate(valid().phone("3001234567").build());
        CustomerRegistrationRules.validate(valid().phone("3159876543").build());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "ABC1234567", "300123456", "2001234567", "30012345678", "300-123-4567"})
    void shouldRejectInvalidPhone(String phone) {
        InvalidRegistrationException exception = assertThrows(
                InvalidRegistrationException.class,
                () -> CustomerRegistrationRules.validate(valid().phone(phone).build()));
        assertEquals(
                phone == null || phone.isBlank()
                        ? "El celular no puede quedar vacío."
                        : "Ingresa un número de celular válido.",
                exception.getMessage());
    }

    @Test
    void shouldAcceptValidPassword() {
        CustomerRegistrationRules.validate(valid().password("Luis123!").build());
    }

    @ParameterizedTest
    @ValueSource(strings = {"luis123!", "LUIS123!", "Luis1234", "Luis!", "Luis123!xxxxx"})
    void shouldRejectInvalidPassword(String password) {
        InvalidRegistrationException exception = assertThrows(
                InvalidRegistrationException.class,
                () -> CustomerRegistrationRules.validate(valid().password(password).build()));
        assertEquals("La contraseña no cumple los requisitos indicados.", exception.getMessage());
    }

    private static CommandBuilder valid() {
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

        private CommandBuilder documentType(String documentType) {
            this.documentType = documentType;
            return this;
        }

        private CommandBuilder documentNumber(String documentNumber) {
            this.documentNumber = documentNumber;
            return this;
        }

        private CommandBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        private CommandBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        private CommandBuilder email(String email) {
            this.email = email;
            return this;
        }

        private CommandBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        private CommandBuilder password(String password) {
            this.password = password;
            return this;
        }

        private RegisterCustomerCommand build() {
            return new RegisterCustomerCommand(
                    documentType, documentNumber, firstName, lastName, email, phone, password);
        }
    }
}

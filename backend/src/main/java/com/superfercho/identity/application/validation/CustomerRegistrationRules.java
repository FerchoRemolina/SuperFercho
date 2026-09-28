package com.superfercho.identity.application.validation;

import com.superfercho.identity.application.dto.RegisterCustomerCommand;
import com.superfercho.identity.application.exception.InvalidRegistrationException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Registration rules for Customer signup. Mirrors frontend register-validation.
 */
public final class CustomerRegistrationRules {

    public static final int NAME_MAX = 20;
    public static final int DOCUMENT_NUMBER_MAX = 10;
    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 12;

    private static final Set<String> DOCUMENT_TYPES = Set.of("CC", "CE");
    private static final Pattern DOCUMENT_NUMBER = Pattern.compile("^\\d{1,10}$");
    private static final Pattern PERSON_NAME =
            Pattern.compile("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?: [A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^3\\d{9}$");
    private static final Pattern PASSWORD_UPPER = Pattern.compile("[A-ZÁÉÍÓÚÜÑ]");
    private static final Pattern PASSWORD_LOWER = Pattern.compile("[a-záéíóúüñ]");
    private static final Pattern PASSWORD_DIGIT = Pattern.compile("\\d");
    private static final Pattern PASSWORD_SPECIAL = Pattern.compile("[^A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9]");

    private CustomerRegistrationRules() {}

    public static String normalizePersonName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    public static String normalizeEmail(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public static ValidatedRegistration validate(RegisterCustomerCommand command) {
        requireDocumentType(command.documentType());
        requireDocumentNumber(command.documentNumber());
        String firstName = normalizePersonName(command.firstName());
        requireFirstName(firstName);
        String lastName = normalizePersonName(command.lastName());
        requireLastName(lastName);
        String email = normalizeEmail(command.email());
        requireEmail(email);
        requirePhone(command.phone());
        requirePassword(command.password());
        return new ValidatedRegistration(
                command.documentType(),
                command.documentNumber(),
                firstName,
                lastName,
                email,
                command.phone(),
                command.password());
    }

    /** Shared document validation for registration and document-based password recovery. */
    public static void requireDocument(String documentType, String documentNumber) {
        requireDocumentType(documentType);
        requireDocumentNumber(documentNumber);
    }

    private static void requireDocumentType(String value) {
        if (value == null || value.isBlank() || !DOCUMENT_TYPES.contains(value)) {
            throw new InvalidRegistrationException("Selecciona un tipo de documento.");
        }
    }

    private static void requireDocumentNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidRegistrationException("El número de documento no puede quedar vacío.");
        }
        if (!DOCUMENT_NUMBER.matcher(value).matches()) {
            throw new InvalidRegistrationException("Ingresa un número de documento válido.");
        }
    }

    private static void requireFirstName(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidRegistrationException("El nombre no puede quedar vacío.");
        }
        if (value.length() > NAME_MAX || !PERSON_NAME.matcher(value).matches()) {
            throw new InvalidRegistrationException("Introduce un nombre válido.");
        }
    }

    private static void requireLastName(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidRegistrationException("El apellido no puede quedar vacío.");
        }
        if (value.length() > NAME_MAX || !PERSON_NAME.matcher(value).matches()) {
            throw new InvalidRegistrationException("Introduce un apellido válido.");
        }
    }

    private static void requireEmail(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidRegistrationException("El correo no puede quedar vacío.");
        }
        if (!EMAIL.matcher(value).matches()) {
            throw new InvalidRegistrationException("Ingresa un correo electrónico válido.");
        }
    }

    private static void requirePhone(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidRegistrationException("El celular no puede quedar vacío.");
        }
        if (!PHONE.matcher(value).matches()) {
            throw new InvalidRegistrationException("Ingresa un número de celular válido.");
        }
    }

    /** Shared password policy for registration and password recovery reset. */
    public static void requirePassword(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidRegistrationException("La contraseña no puede quedar vacía.");
        }
        if (value.length() < PASSWORD_MIN
                || value.length() > PASSWORD_MAX
                || !PASSWORD_UPPER.matcher(value).find()
                || !PASSWORD_LOWER.matcher(value).find()
                || !PASSWORD_DIGIT.matcher(value).find()
                || !PASSWORD_SPECIAL.matcher(value).find()) {
            throw new InvalidRegistrationException("La contraseña no cumple los requisitos indicados.");
        }
    }

    public record ValidatedRegistration(
            String documentType,
            String documentNumber,
            String firstName,
            String lastName,
            String email,
            String phone,
            String password) {}
}

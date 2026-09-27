import { describe, expect, it } from "vitest";
import {
  digitsOnly,
  isPasswordValid,
  normalizeRegisterPersonName,
  passwordRequirements,
  validateDocumentNumber,
  validateDocumentType,
  validateEmail,
  validateFirstName,
  validateLastName,
  validatePassword,
  validatePhone,
  validateRegisterForm,
} from "@/features/auth/register-validation";

describe("register document type", () => {
  it("rejects empty and accepts supported types", () => {
    expect(validateDocumentType("")).toBe("Selecciona un tipo de documento.");
    expect(validateDocumentType("CC")).toBeNull();
    expect(validateDocumentType("CE")).toBeNull();
    expect(validateDocumentType("TI")).toBe("Selecciona un tipo de documento.");
  });
});

describe("register document number", () => {
  it("validates empty, digits and max length", () => {
    expect(validateDocumentNumber("")).toBe(
      "El número de documento no puede quedar vacío.",
    );
    expect(validateDocumentNumber("12345678")).toBeNull();
    expect(validateDocumentNumber("1234567890")).toBeNull();
    expect(validateDocumentNumber("12345678901")).toBe(
      "Ingresa un número de documento válido.",
    );
    expect(validateDocumentNumber("12A34")).toBe(
      "Ingresa un número de documento válido.",
    );
  });
});

describe("register first and last name", () => {
  it("accepts valid names and rejects empty or invalid", () => {
    expect(validateFirstName("")).toBe("El nombre no puede quedar vacío.");
    expect(validateFirstName("   ")).toBe("El nombre no puede quedar vacío.");
    expect(validateFirstName("Luis")).toBeNull();
    expect(validateFirstName("Juan Carlos")).toBeNull();
    expect(validateFirstName("María Fernanda")).toBeNull();
    expect(validateFirstName("José María")).toBeNull();
    expect(validateFirstName("Luis123")).toBe("Introduce un nombre válido.");
    expect(validateFirstName("Luis  Alberto")).toBe(
      "Introduce un nombre válido.",
    );
    expect(validateFirstName("A".repeat(21))).toBe(
      "Introduce un nombre válido.",
    );

    expect(validateLastName("")).toBe("El apellido no puede quedar vacío.");
    expect(validateLastName("Remolina")).toBeNull();
    expect(validateLastName("De la Cruz")).toBeNull();
    expect(validateLastName("Remolina@")).toBe(
      "Introduce un apellido válido.",
    );
  });

  it("trims ends without collapsing consecutive spaces", () => {
    expect(normalizeRegisterPersonName("  Luis  ")).toBe("Luis");
    expect(normalizeRegisterPersonName("  Luis  Alberto  ")).toBe(
      "Luis  Alberto",
    );
  });
});

describe("register email", () => {
  it("requires a non-empty valid email", () => {
    expect(validateEmail("")).toBe("El correo no puede quedar vacío.");
    expect(validateEmail("bad")).toBe("Ingresa un correo electrónico válido.");
    expect(validateEmail("ada@identity.test")).toBeNull();
  });
});

describe("register phone", () => {
  it("requires a Colombian mobile number", () => {
    expect(validatePhone("")).toBe("El celular no puede quedar vacío.");
    expect(validatePhone("300123456")).toBe(
      "Ingresa un número de celular válido.",
    );
    expect(validatePhone("2001234567")).toBe(
      "Ingresa un número de celular válido.",
    );
    expect(validatePhone("3001234567")).toBeNull();
  });
});

describe("register password", () => {
  it("distinguishes empty from invalid", () => {
    expect(validatePassword("")).toBe("La contraseña no puede quedar vacía.");
    expect(validatePassword("short")).toBe(
      "La contraseña no cumple los requisitos indicados.",
    );
    expect(validatePassword("Luis123!")).toBeNull();
    expect(isPasswordValid("Luis123!")).toBe(true);
    expect(passwordRequirements("Luis123").find((r) => r.id === "special")?.met)
      .toBe(false);
  });
});

describe("validateRegisterForm", () => {
  it("aggregates field errors", () => {
    const errors = validateRegisterForm({
      documentType: "",
      documentNumber: "",
      firstName: "",
      lastName: "",
      email: "",
      phone: "",
      password: "",
    });
    expect(errors.documentType).toBeTruthy();
    expect(errors.documentNumber).toBeTruthy();
    expect(errors.firstName).toBe("El nombre no puede quedar vacío.");
    expect(errors.lastName).toBe("El apellido no puede quedar vacío.");
    expect(errors.email).toBeTruthy();
    expect(errors.phone).toBeTruthy();
    expect(errors.password).toBe("La contraseña no puede quedar vacía.");
  });

  it("digitsOnly strips non-digits", () => {
    expect(digitsOnly("30a0-123")).toBe("300123");
  });
});

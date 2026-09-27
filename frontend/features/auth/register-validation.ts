import { REGISTER_DOCUMENT_TYPES } from "@/features/auth/api";

export const REGISTER_NAME_MAX = 20;
export const REGISTER_DOCUMENT_NUMBER_MAX = 10;
export const REGISTER_PASSWORD_MIN = 8;
export const REGISTER_PASSWORD_MAX = 12;
export const REGISTER_PHONE_LENGTH = 10;

const DOCUMENT_TYPE_VALUES = new Set(
  REGISTER_DOCUMENT_TYPES.map((type) => type.value),
);

const PERSON_NAME_PATTERN =
  /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?: [A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$/;

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const PHONE_PATTERN = /^3\d{9}$/;

const DOCUMENT_NUMBER_PATTERN = /^\d{1,10}$/;

export type RegisterFormValues = {
  documentType: string;
  documentNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  password: string;
};

export type RegisterFieldErrors = Partial<
  Record<keyof RegisterFormValues, string>
>;

export type PasswordRequirementId =
  | "length"
  | "upper"
  | "lower"
  | "digit"
  | "special";

export type PasswordRequirement = {
  id: PasswordRequirementId;
  label: string;
  met: boolean;
};

export function normalizeRegisterPersonName(value: string): string {
  return value.trim();
}

export function digitsOnly(value: string): string {
  return value.replace(/\D/g, "");
}

export function passwordRequirements(password: string): PasswordRequirement[] {
  return [
    {
      id: "length",
      label: "Entre 8 y 12 caracteres",
      met:
        password.length >= REGISTER_PASSWORD_MIN &&
        password.length <= REGISTER_PASSWORD_MAX,
    },
    {
      id: "upper",
      label: "Una letra mayúscula",
      met: /[A-ZÁÉÍÓÚÜÑ]/.test(password),
    },
    {
      id: "lower",
      label: "Una letra minúscula",
      met: /[a-záéíóúüñ]/.test(password),
    },
    {
      id: "digit",
      label: "Un número",
      met: /\d/.test(password),
    },
    {
      id: "special",
      label: "Un carácter especial",
      met: /[^A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9]/.test(password),
    },
  ];
}

export function isPasswordValid(password: string): boolean {
  return passwordRequirements(password).every((item) => item.met);
}

export function validateDocumentType(value: string): string | null {
  if (!value.trim()) {
    return "Selecciona un tipo de documento.";
  }
  if (!DOCUMENT_TYPE_VALUES.has(value as "CC" | "CE")) {
    return "Selecciona un tipo de documento.";
  }
  return null;
}

export function validateDocumentNumber(value: string): string | null {
  if (!value.trim()) {
    return "El número de documento no puede quedar vacío.";
  }
  if (!DOCUMENT_NUMBER_PATTERN.test(value)) {
    return "Ingresa un número de documento válido.";
  }
  return null;
}

function validatePersonName(
  value: string,
  emptyMessage: string,
  invalidMessage: string,
): string | null {
  const normalized = normalizeRegisterPersonName(value);
  if (!normalized) {
    return emptyMessage;
  }
  if (
    normalized.length > REGISTER_NAME_MAX ||
    !PERSON_NAME_PATTERN.test(normalized)
  ) {
    return invalidMessage;
  }
  return null;
}

export function validateFirstName(value: string): string | null {
  return validatePersonName(
    value,
    "El nombre no puede quedar vacío.",
    "Introduce un nombre válido.",
  );
}

export function validateLastName(value: string): string | null {
  return validatePersonName(
    value,
    "El apellido no puede quedar vacío.",
    "Introduce un apellido válido.",
  );
}

export function validateEmail(value: string): string | null {
  const trimmed = value.trim();
  if (!trimmed) {
    return "El correo no puede quedar vacío.";
  }
  if (!EMAIL_PATTERN.test(trimmed)) {
    return "Ingresa un correo electrónico válido.";
  }
  return null;
}

export function validatePhone(value: string): string | null {
  if (!value.trim()) {
    return "El celular no puede quedar vacío.";
  }
  if (!PHONE_PATTERN.test(value)) {
    return "Ingresa un número de celular válido.";
  }
  return null;
}

export function validatePassword(value: string): string | null {
  if (!value) {
    return "La contraseña no puede quedar vacía.";
  }
  if (!isPasswordValid(value)) {
    return "La contraseña no cumple los requisitos indicados.";
  }
  return null;
}

export function validateRegisterForm(
  values: RegisterFormValues,
): RegisterFieldErrors {
  const errors: RegisterFieldErrors = {};
  const documentType = validateDocumentType(values.documentType);
  const documentNumber = validateDocumentNumber(values.documentNumber);
  const firstName = validateFirstName(values.firstName);
  const lastName = validateLastName(values.lastName);
  const email = validateEmail(values.email);
  const phone = validatePhone(values.phone);
  const password = validatePassword(values.password);

  if (documentType) errors.documentType = documentType;
  if (documentNumber) errors.documentNumber = documentNumber;
  if (firstName) errors.firstName = firstName;
  if (lastName) errors.lastName = lastName;
  if (email) errors.email = email;
  if (phone) errors.phone = phone;
  if (password) errors.password = password;
  return errors;
}

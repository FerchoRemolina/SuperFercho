import { request } from "@/shared/api/client";
import type { Role, Session } from "@/shared/session/session";

/** Mirrors AuthenticateUserRequest. */
export type AuthenticateUserRequest = {
  email: string;
  password: string;
};

/** Mirrors AuthenticationRestResponse. */
export type AuthenticationRestResponse = {
  userId: string;
  role: Role;
  firstName: string;
  lastName: string;
  accessToken: string;
  expiresAt: string;
};

/** Mirrors RegisterCustomerRequest. */
export type RegisterCustomerRequest = {
  documentType: string;
  documentNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  password: string;
};

export const REGISTER_DOCUMENT_TYPES = [
  { value: "CC", label: "Cédula de ciudadanía" },
  { value: "CE", label: "Cédula de extranjería" },
] as const;

/** Mirrors RegisteredCustomerRestResponse. */
export type RegisteredCustomerRestResponse = {
  id: string;
  documentType: string;
  documentNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  role: Role;
  status: "ACTIVE" | "INACTIVE";
  createdAt: string;
};

export async function login(
  body: AuthenticateUserRequest,
): Promise<AuthenticationRestResponse> {
  return request<AuthenticationRestResponse>("/auth/login", {
    method: "POST",
    body,
  });
}

export async function registerCustomer(
  body: RegisterCustomerRequest,
): Promise<RegisteredCustomerRestResponse> {
  return request<RegisteredCustomerRestResponse>("/customers", {
    method: "POST",
    body,
  });
}

export type RequestPasswordRecoveryRequest = {
  email: string;
};

export type PasswordRecoveryRequestRestResponse = {
  message: string;
};

export type ResetPasswordRequest = {
  token: string;
  newPassword: string;
  confirmPassword: string;
};

export async function requestPasswordRecovery(
  body: RequestPasswordRecoveryRequest,
): Promise<PasswordRecoveryRequestRestResponse> {
  return request<PasswordRecoveryRequestRestResponse>("/auth/password-recovery", {
    method: "POST",
    body,
    anonymous: true,
  });
}

export async function resetPassword(body: ResetPasswordRequest): Promise<void> {
  await request<void>("/auth/password-recovery/reset", {
    method: "POST",
    body,
    anonymous: true,
  });
}

export function sessionFromAuthentication(
  response: AuthenticationRestResponse,
): NonNullable<Session> {
  const firstName = response.firstName?.trim();
  const lastName = response.lastName?.trim() ?? "";
  return {
    userId: response.userId,
    role: response.role,
    accessToken: response.accessToken,
    expiresAt: response.expiresAt,
    ...(firstName ? { firstName } : {}),
    ...(lastName ? { lastName } : {}),
  };
}

export function homePathForRole(role: Role): string {
  return role === "ADMIN" ? "/admin" : "/";
}

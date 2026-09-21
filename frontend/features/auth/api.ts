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
  accessToken: string;
  expiresAt: string;
};

/** Mirrors RegisterCustomerRequest. */
export type RegisterCustomerRequest = {
  documentType: string;
  documentNumber: string;
  fullName: string;
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
  fullName: string;
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

export function sessionFromAuthentication(
  response: AuthenticationRestResponse,
): NonNullable<Session> {
  return {
    userId: response.userId,
    role: response.role,
    accessToken: response.accessToken,
    expiresAt: response.expiresAt,
  };
}

export function homePathForRole(role: Role): string {
  return role === "ADMIN" ? "/admin" : "/";
}

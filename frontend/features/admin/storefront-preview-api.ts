import { request } from "@/shared/api/client";
import type { Role, Session } from "@/shared/session/session";
import type { PreviewMeta } from "@/shared/session/preview-session";

/** Mirrors StorefrontPreviewSessionRestResponse. */
export type StorefrontPreviewSessionRestResponse = {
  previewId: string;
  adminUserId: string;
  temporaryCustomerId: string;
  role: Role;
  status: "ACTIVE" | "CLOSED";
  previewCreatedAt: string;
  previewExpiresAt: string;
  remainingSeconds: number;
  accessToken: string;
  accessTokenExpiresAt: string;
  adminAccessToken: string;
  adminAccessTokenExpiresAt: string;
};

/** Mirrors StorefrontPreviewStatusRestResponse. */
export type StorefrontPreviewStatusRestResponse = {
  previewId: string;
  adminUserId: string;
  temporaryCustomerId: string;
  status: "ACTIVE" | "CLOSED";
  previewCreatedAt: string;
  previewExpiresAt: string;
  remainingSeconds: number;
  usable: boolean;
};

export async function startStorefrontPreview(): Promise<StorefrontPreviewSessionRestResponse> {
  return request<StorefrontPreviewSessionRestResponse>(
    "/admin/storefront-preview",
    { method: "POST" },
  );
}

export async function getStorefrontPreview(): Promise<StorefrontPreviewStatusRestResponse> {
  return request<StorefrontPreviewStatusRestResponse>(
    "/admin/storefront-preview",
    { method: "GET" },
  );
}

export async function exitStorefrontPreview(
  adminAccessToken?: string,
): Promise<void> {
  const headers: Record<string, string> = {};
  if (adminAccessToken) {
    headers.Authorization = `Bearer ${adminAccessToken}`;
  }
  await request<void>("/admin/storefront-preview/exit", {
    method: "POST",
    headers,
    // Prefer the explicit Admin bearer while a Customer preview JWT is active.
    anonymous: Boolean(adminAccessToken),
  });
}

export function sessionFromStorefrontPreview(
  response: StorefrontPreviewSessionRestResponse,
): NonNullable<Session> {
  return {
    userId: response.temporaryCustomerId,
    role: "CUSTOMER",
    accessToken: response.accessToken,
    expiresAt: response.accessTokenExpiresAt,
  };
}

/** Fresh Admin JWT issued at Ver tienda — stash this, not the pre-preview Admin token. */
export function adminSessionFromStorefrontPreview(
  response: StorefrontPreviewSessionRestResponse,
): NonNullable<Session> {
  return {
    userId: response.adminUserId,
    role: "ADMIN",
    accessToken: response.adminAccessToken,
    expiresAt: response.adminAccessTokenExpiresAt,
  };
}

export function previewMetaFromStorefrontPreview(
  response: StorefrontPreviewSessionRestResponse,
): PreviewMeta {
  return {
    previewId: response.previewId,
    previewExpiresAt: response.previewExpiresAt,
    temporaryCustomerId: response.temporaryCustomerId,
  };
}

import {
  adminSessionFromStorefrontPreview,
  previewMetaFromStorefrontPreview,
  sessionFromStorefrontPreview,
  type StorefrontPreviewSessionRestResponse,
} from "@/features/admin/storefront-preview-api";
import {
  clearPreviewState,
  clearStashedAdminSession,
  getPreviewMeta,
  isStorefrontPreviewActive,
  readStashedAdminSession,
  setPreviewMeta,
  stashAdminSession,
  type PreviewMeta,
} from "@/shared/session/preview-session";
import {
  clearSession,
  getSession,
  isExpired,
  setSession,
  type Session,
} from "@/shared/session/session";

export type PreviewUnauthenticatedAction = "restore-admin" | "logout";

/**
 * ADMIN → CUSTOMER preview.
 * Stashes the fresh Admin JWT from the start response (aligned with preview window),
 * writes preview meta, activates temporary Customer session.
 * Caller must clear React Query cache and navigate to "/".
 */
export function activateStorefrontPreviewSession(
  adminSession: NonNullable<Session>,
  response: StorefrontPreviewSessionRestResponse,
): NonNullable<Session> {
  if (adminSession.role !== "ADMIN") {
    throw new Error("Solo un ADMIN puede iniciar Ver tienda");
  }
  const freshAdmin = adminSessionFromStorefrontPreview(response);
  stashAdminSession(freshAdmin);
  setPreviewMeta(previewMetaFromStorefrontPreview(response));
  const customer = sessionFromStorefrontPreview(response);
  setSession(customer);
  return customer;
}

/**
 * CUSTOMER preview → ADMIN without destroying the backend preview.
 * Moves Admin back into the active session and drops the stash
 * (stash exists only while the temporary Customer session is active).
 * Keeps preview-meta so "Ver tienda" can reuse the ACTIVE preview.
 * Caller clears cache and navigates to /admin.
 */
export function restoreAdminFromPreviewStash(
  now = Date.now(),
): NonNullable<Session> | null {
  const admin = readStashedAdminSession();
  if (!admin || isExpired(admin, now)) {
    if (admin && isExpired(admin, now)) {
      clearStashedAdminSession();
    }
    return null;
  }
  setSession(admin);
  clearStashedAdminSession();
  return admin;
}

/**
 * After POST /exit (or expiry): drop preview metadata and restore Admin if possible.
 * Caller clears cache and navigates.
 */
export function closePreviewAndRestoreAdmin(
  now = Date.now(),
): {
  admin: NonNullable<Session> | null;
} {
  const stashed = readStashedAdminSession();
  const current = getSession();
  const candidate =
    stashed && !isExpired(stashed, now)
      ? stashed
      : current?.role === "ADMIN" && !isExpired(current, now)
        ? current
        : null;
  clearPreviewState();
  if (candidate) {
    setSession(candidate);
  } else {
    clearSession();
  }
  return { admin: candidate };
}

/**
 * Full logout cleanup after optional /exit attempt.
 */
export function clearAllSessionState(): void {
  clearPreviewState();
  clearSession();
}

/**
 * 401 UNAUTHENTICATED while the temporary Customer preview session is active
 * (valid Admin stash still present) → restore Admin, not /login.
 */
export function resolvePreviewUnauthenticatedAction(
  meta: PreviewMeta | null = getPreviewMeta(),
  now = Date.now(),
): PreviewUnauthenticatedAction {
  const admin = readStashedAdminSession();
  if (admin && meta && !isExpired(admin, now)) {
    return "restore-admin";
  }
  return "logout";
}

export function applyPreviewUnauthenticatedRestore(
  now = Date.now(),
): NonNullable<Session> | null {
  if (resolvePreviewUnauthenticatedAction(getPreviewMeta(), now) !== "restore-admin") {
    return null;
  }
  const admin = readStashedAdminSession();
  if (!admin || isExpired(admin, now)) {
    return null;
  }
  clearPreviewState();
  setSession(admin);
  return admin;
}

export function adminTokenForPreviewExit(now = Date.now()): string | null {
  const stashed = readStashedAdminSession();
  if (stashed?.accessToken && !isExpired(stashed, now)) {
    return stashed.accessToken;
  }
  const current = getSession();
  if (current?.role === "ADMIN" && !isExpired(current, now)) {
    return current.accessToken;
  }
  return null;
}

export function hasActivePreviewState(): boolean {
  return getPreviewMeta() != null || readStashedAdminSession() != null;
}

/**
 * Align sessionStorage keys after load / F5.
 * Does not call backend exit; sweeper remains authority for abandoned ACTIVE previews.
 */
export function reconcilePreviewSessionOnHydrate(now = Date.now()): {
  restoredAdmin: boolean;
} {
  const session = getSession();
  const meta = getPreviewMeta();
  const stash = readStashedAdminSession();

  if (stash && isExpired(stash, now)) {
    clearStashedAdminSession();
  }

  const freshStash = readStashedAdminSession();

  if (!session) {
    if (freshStash && meta) {
      setSession(freshStash);
      clearStashedAdminSession();
      return { restoredAdmin: true };
    }
    if (meta && !freshStash) {
      // Orphan meta without usable Admin stash — drop FE markers only.
      clearPreviewState();
    }
    return { restoredAdmin: false };
  }

  if (session.role === "CUSTOMER" && meta) {
    if (isStorefrontPreviewActive(session, meta)) {
      return { restoredAdmin: false };
    }
    // Meta does not match this customer — drop meta only.
    clearPreviewState();
    if (freshStash) {
      clearStashedAdminSession();
    }
    return { restoredAdmin: false };
  }

  if (session.role === "ADMIN" && meta) {
    // Admin restored with ACTIVE preview meta — keep meta, drop any leftover stash.
    if (freshStash) {
      clearStashedAdminSession();
    }
    return { restoredAdmin: false };
  }

  if (session.role === "CUSTOMER" && !meta && freshStash) {
    clearStashedAdminSession();
  }

  return { restoredAdmin: false };
}

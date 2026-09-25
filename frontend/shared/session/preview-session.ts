import {
  formatSessionRemaining,
  remainingMsUntil,
} from "@/shared/session/session-countdown";
import {
  getSessionPersistence,
  type Session,
} from "@/shared/session/session";

export const ADMIN_SESSION_STORAGE_KEY = "superfercho.admin-session";
export const PREVIEW_META_STORAGE_KEY = "superfercho.preview-meta";

export type PreviewMeta = {
  previewId: string;
  previewExpiresAt: string;
  temporaryCustomerId: string;
};

const metaListeners = new Set<() => void>();

export function subscribeToPreviewMeta(listener: () => void): () => void {
  metaListeners.add(listener);
  return () => {
    metaListeners.delete(listener);
  };
}

function emitPreviewMeta(): void {
  for (const listener of metaListeners) {
    listener();
  }
}

function parseSessionShape(value: unknown): NonNullable<Session> | null {
  if (typeof value !== "object" || value === null) {
    return null;
  }
  const record = value as Record<string, unknown>;
  if (
    typeof record.userId !== "string" ||
    record.role !== "ADMIN" ||
    typeof record.accessToken !== "string" ||
    typeof record.expiresAt !== "string"
  ) {
    return null;
  }
  return {
    userId: record.userId,
    role: "ADMIN",
    accessToken: record.accessToken,
    expiresAt: record.expiresAt,
  };
}

function parsePreviewMeta(value: unknown): PreviewMeta | null {
  if (typeof value !== "object" || value === null) {
    return null;
  }
  const record = value as Record<string, unknown>;
  if (
    typeof record.previewId !== "string" ||
    typeof record.previewExpiresAt !== "string" ||
    typeof record.temporaryCustomerId !== "string"
  ) {
    return null;
  }
  return {
    previewId: record.previewId,
    previewExpiresAt: record.previewExpiresAt,
    temporaryCustomerId: record.temporaryCustomerId,
  };
}

export function stashAdminSession(session: NonNullable<Session>): void {
  const persistence = getSessionPersistence();
  if (!persistence || session.role !== "ADMIN") {
    return;
  }
  persistence.setItem(ADMIN_SESSION_STORAGE_KEY, JSON.stringify(session));
}

export function readStashedAdminSession(): NonNullable<Session> | null {
  const persistence = getSessionPersistence();
  if (!persistence) {
    return null;
  }
  const raw = persistence.getItem(ADMIN_SESSION_STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    return parseSessionShape(JSON.parse(raw));
  } catch {
    persistence.removeItem(ADMIN_SESSION_STORAGE_KEY);
    return null;
  }
}

export function clearStashedAdminSession(): void {
  getSessionPersistence()?.removeItem(ADMIN_SESSION_STORAGE_KEY);
}

export function setPreviewMeta(meta: PreviewMeta): void {
  const persistence = getSessionPersistence();
  if (!persistence) {
    return;
  }
  persistence.setItem(PREVIEW_META_STORAGE_KEY, JSON.stringify(meta));
  emitPreviewMeta();
}

export function getPreviewMeta(): PreviewMeta | null {
  const persistence = getSessionPersistence();
  if (!persistence) {
    return null;
  }
  const raw = persistence.getItem(PREVIEW_META_STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    return parsePreviewMeta(JSON.parse(raw));
  } catch {
    persistence.removeItem(PREVIEW_META_STORAGE_KEY);
    return null;
  }
}

export function clearPreviewMeta(): void {
  getSessionPersistence()?.removeItem(PREVIEW_META_STORAGE_KEY);
  emitPreviewMeta();
}

export function clearPreviewState(): void {
  clearStashedAdminSession();
  clearPreviewMeta();
}

export function isStorefrontPreviewActive(
  session: Session,
  meta: PreviewMeta | null = getPreviewMeta(),
): boolean {
  return (
    session?.role === "CUSTOMER" &&
    meta != null &&
    meta.temporaryCustomerId === session.userId
  );
}

export function formatPreviewRemaining(remainingMs: number): string {
  if (remainingMs <= 0) {
    return "La preview ya no es usable";
  }
  const totalSeconds = Math.floor(remainingMs / 1000);
  if (totalSeconds < 60) {
    return "Queda menos de 1 minuto";
  }
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  if (minutes <= 2) {
    return `Quedan ${minutes}:${String(seconds).padStart(2, "0")}`;
  }
  return `${minutes}:${String(seconds).padStart(2, "0")} restantes`;
}

export function previewRemainingMs(
  meta: PreviewMeta,
  now = Date.now(),
): number {
  return remainingMsUntil(meta.previewExpiresAt, now);
}

export { formatSessionRemaining, remainingMsUntil };

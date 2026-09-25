export type Role = "CUSTOMER" | "ADMIN";

export type Session = {
  userId: string;
  role: Role;
  accessToken: string;
  expiresAt: string;
} | null;

export const SESSION_STORAGE_KEY = "superfercho.session";

export type SessionPersistence = {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
  removeItem(key: string): void;
};

let memory: Session = null;
let persistence: SessionPersistence | null = null;
const listeners = new Set<() => void>();

export function configureSessionPersistence(
  next: SessionPersistence | null,
): void {
  persistence = next;
}

export function subscribeToSession(listener: () => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

export function getSession(): Session {
  return memory;
}

export function getAccessToken(): string | null {
  const session = getFreshSession();
  return session?.accessToken ?? null;
}

export function setSession(session: NonNullable<Session>): void {
  memory = session;
  writePersistence(session);
  emit();
}

export function clearSession(): void {
  memory = null;
  writePersistence(null);
  emit();
}

export function hydrateSession(now = Date.now()): Session {
  const stored = readPersistence();
  if (!stored || isExpired(stored, now)) {
    memory = null;
    writePersistence(null);
    emit();
    return null;
  }
  memory = stored;
  emit();
  return memory;
}

export function isExpired(
  session: NonNullable<Session>,
  now = Date.now(),
): boolean {
  const expiresAt = Date.parse(session.expiresAt);
  return Number.isNaN(expiresAt) || expiresAt <= now;
}

function getFreshSession(now = Date.now()): Session {
  if (!memory) {
    return null;
  }
  if (isExpired(memory, now)) {
    clearSession();
    return null;
  }
  return memory;
}

function emit(): void {
  for (const listener of listeners) {
    listener();
  }
}

function storage(): SessionPersistence | null {
  if (persistence) {
    return persistence;
  }
  if (typeof window === "undefined") {
    return null;
  }
  return window.sessionStorage;
}

/** Shared storage accessor for session-adjacent keys (admin stash, preview meta). */
export function getSessionPersistence(): SessionPersistence | null {
  return storage();
}

function readPersistence(): Session {
  const store = storage();
  if (!store) {
    return null;
  }
  const raw = store.getItem(SESSION_STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    const parsed: unknown = JSON.parse(raw);
    return parseSession(parsed);
  } catch {
    store.removeItem(SESSION_STORAGE_KEY);
    return null;
  }
}

function writePersistence(session: Session): void {
  const store = storage();
  if (!store) {
    return;
  }
  if (!session) {
    store.removeItem(SESSION_STORAGE_KEY);
    return;
  }
  store.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
}

function parseSession(value: unknown): Session {
  if (typeof value !== "object" || value === null) {
    return null;
  }
  const record = value as Record<string, unknown>;
  if (
    typeof record.userId !== "string" ||
    (record.role !== "CUSTOMER" && record.role !== "ADMIN") ||
    typeof record.accessToken !== "string" ||
    typeof record.expiresAt !== "string"
  ) {
    return null;
  }
  return {
    userId: record.userId,
    role: record.role,
    accessToken: record.accessToken,
    expiresAt: record.expiresAt,
  };
}

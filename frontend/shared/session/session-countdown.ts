/**
 * Shared session/preview countdown formatting (JWT expiresAt or previewExpiresAt).
 */

export function remainingMsUntil(expiresAt: string, now = Date.now()): number {
  const expires = Date.parse(expiresAt);
  if (Number.isNaN(expires)) {
    return 0;
  }
  return Math.max(0, expires - now);
}

export function formatSessionRemaining(remainingMs: number): string {
  if (remainingMs <= 0) {
    return "Sesión expirada";
  }
  const totalSeconds = Math.floor(remainingMs / 1000);
  if (totalSeconds < 60) {
    return "Menos de 1 min";
  }
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${String(seconds).padStart(2, "0")}`;
}

export type SessionCountdownTone = "green" | "yellow" | "red";

/**
 * Color del temporizador según el tiempo REAL restante de la sesión,
 * evaluado sobre los segundos mostrados: verde > 5:00 · amarillo 2:00–5:00
 * (incluidos) · rojo < 2:00. Ejemplos: 05:01 verde · 05:00 amarillo ·
 * 02:00 amarillo · 01:59 rojo.
 */
export function sessionCountdownTone(remainingMs: number): SessionCountdownTone {
  const totalSeconds = Math.floor(remainingMs / 1000);
  if (totalSeconds > 300) {
    return "green";
  }
  if (totalSeconds >= 120) {
    return "yellow";
  }
  return "red";
}

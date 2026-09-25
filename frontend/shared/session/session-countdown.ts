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

/**
 * Internal path only. Rejects protocol-relative and external URLs.
 */
export function safeNextPath(value: string | null | undefined): string | null {
  if (!value || !value.startsWith("/") || value.startsWith("//")) {
    return null;
  }
  if (value.includes("://") || value.includes("\\")) {
    return null;
  }
  return value;
}

export function loginPathWithNext(currentPath: string): string {
  const next = safeNextPath(currentPath);
  return next ? `/login?next=${encodeURIComponent(next)}` : "/login";
}

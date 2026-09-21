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

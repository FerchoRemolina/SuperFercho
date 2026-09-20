export function getBrowserApiBaseUrl(): string {
  return process.env.NEXT_PUBLIC_API_BASE_URL ?? "";
}

export function getServerApiOrigin(): string {
  return process.env.SUPERFERCHO_API_ORIGIN ?? "http://localhost:8080";
}

export function getApiBaseUrl(): string {
  if (typeof window === "undefined") {
    return getServerApiOrigin();
  }
  return getBrowserApiBaseUrl();
}

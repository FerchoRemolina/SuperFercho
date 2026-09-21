import { getApiBaseUrl } from "@/shared/config/env";
import {
  ApiError,
  parseApiProblem,
  type ApiProblem,
} from "@/shared/errors/api-problem";
import { clearSession, getAccessToken } from "@/shared/session/session";

export { ApiError };

export type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

export type RequestOptions = {
  method?: HttpMethod;
  body?: unknown;
  headers?: Record<string, string>;
  idempotencyKey?: string;
  anonymous?: boolean;
};

let unauthenticatedHandler: (() => void) | null = null;

export function setUnauthenticatedHandler(handler: (() => void) | null): void {
  unauthenticatedHandler = handler;
}

export async function request<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const token = getAccessToken();
  const headers = new Headers(options.headers);
  headers.set("Accept", "application/json, application/problem+json");

  if (options.body !== undefined) {
    headers.set("Content-Type", "application/json");
  }
  if (token && !options.anonymous && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  if (options.idempotencyKey) {
    headers.set("Idempotency-Key", options.idempotencyKey);
  }

  const response = await fetch(resolveUrl(path), {
    method: options.method ?? "GET",
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });

  if (response.ok) {
    return parseSuccess<T>(response);
  }

  const problem = await parseFailure(response);
  handleAuthFailure(problem);
  throw new ApiError(problem);
}

function resolveUrl(path: string): string {
  const withLeadingSlash = path.startsWith("/") ? path : `/${path}`;
  const apiPath = withLeadingSlash.startsWith("/api/v1")
    ? withLeadingSlash
    : `/api/v1${withLeadingSlash}`;
  return `${getApiBaseUrl()}${apiPath}`;
}

async function parseSuccess<T>(response: Response): Promise<T> {
  if (response.status === 204) {
    return undefined as T;
  }
  const text = await response.text();
  if (!text) {
    return undefined as T;
  }
  return JSON.parse(text) as T;
}

async function parseFailure(response: Response): Promise<ApiProblem> {
  const text = await response.text();
  if (!text) {
    return { status: response.status };
  }
  try {
    return parseApiProblem(response.status, JSON.parse(text));
  } catch {
    return { status: response.status };
  }
}

function handleAuthFailure(problem: ApiProblem): void {
  if (problem.code === "USER_INACTIVE") {
    clearSession();
    return;
  }
  if (problem.status !== 401 || problem.code !== "UNAUTHENTICATED") {
    return;
  }
  clearSession();
  unauthenticatedHandler?.();
}
